package com.sportsplatform.league.application;

import com.sportsplatform.league.domain.Club;
import com.sportsplatform.league.domain.ClubRepository;
import com.sportsplatform.league.domain.CorrelationReApplier;
import com.sportsplatform.league.domain.Fingerprint;
import com.sportsplatform.league.domain.League;
import com.sportsplatform.league.domain.LeagueRepository;
import com.sportsplatform.league.domain.RefusalRecord;
import com.sportsplatform.league.domain.RefusalRecordStore;
import com.sportsplatform.league.domain.SeasonStructure;
import com.sportsplatform.league.domain.SeasonStructureId;
import com.sportsplatform.league.domain.SeasonStructureRepository;
import com.sportsplatform.league.domain.UnitOfWork;

import java.util.Objects;
import java.util.Optional;

/**
 * Apply one prepared structure safely. Produces one of three outcomes:
 * {@link ApplyOutcome.Applied}, {@link ApplyOutcome.Unchanged} or
 * {@link ApplyOutcome.RefusedAndRecorded}.
 *
 * <p>Applied and Unchanged share an acceptance step at the end: the clubs' and the league's
 * display names are upserted, and the correlation re-apply slot is invoked. Refused writes
 * nothing outside the refusal record.</p>
 *
 * <p>The whole thing runs in one database transaction, so a partial structure is never visible.
 * The current season is set only when the league had none; applying another season does not
 * move an already-set current season.</p>
 */
public class ApplyPreparedStructure {

    private final LeagueRepository leagues;
    private final ClubRepository clubs;
    private final SeasonStructureRepository structures;
    private final RefusalRecordStore refusals;
    private final CorrelationReApplier correlations;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public ApplyPreparedStructure(
            LeagueRepository leagues,
            ClubRepository clubs,
            SeasonStructureRepository structures,
            RefusalRecordStore refusals,
            CorrelationReApplier correlations,
            UnitOfWork unitOfWork,
            Clock clock
    ) {
        this.leagues = Objects.requireNonNull(leagues, "leagues");
        this.clubs = Objects.requireNonNull(clubs, "clubs");
        this.structures = Objects.requireNonNull(structures, "structures");
        this.refusals = Objects.requireNonNull(refusals, "refusals");
        this.correlations = Objects.requireNonNull(correlations, "correlations");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ApplyOutcome apply(PreparedStructure input) {
        Objects.requireNonNull(input, "input");

        SeasonStructure structure = input.structure();
        SeasonStructureId target = structure.id();
        Fingerprint incoming = Fingerprint.of(structure);

        return unitOfWork.execute(() -> {
            Optional<Fingerprint> stored = structures.findFingerprint(target);
            if (stored.isEmpty()) {
                writeFreshStructure(input, target, incoming);
                acceptanceStep(input);
                return new ApplyOutcome.Applied(target, incoming);
            }
            if (stored.get().equals(incoming)) {
                acceptanceStep(input);
                return new ApplyOutcome.Unchanged(target, stored.get());
            }
            refusals.save(new RefusalRecord(target, stored.get(), incoming, clock.now()));
            return new ApplyOutcome.RefusedAndRecorded(target, stored.get(), incoming);
        });
    }

    private void writeFreshStructure(PreparedStructure input, SeasonStructureId target, Fingerprint fingerprint) {
        // The composite foreign key on league (id, current_season_id) to
        // season_structure (league_id, season_id) requires the structure to exist before a
        // league row names its current season. Save the league first without a current season
        // (preserving an existing one), then the clubs, then the structure, then — only if the
        // league had no current season — update the league with its new current season.
        League existingLeague = leagues.find(target.league()).orElse(null);
        boolean leagueKeepsCurrentSeason = existingLeague != null
                && existingLeague.currentSeason().isPresent();

        League leaguePreStructure = existingLeague != null
                ? existingLeague
                : League.withoutCurrentSeason(input.league().id(), input.league().name());
        leagues.save(leaguePreStructure);

        for (Club club : input.clubs()) {
            clubs.save(club);
        }

        structures.save(input.structure(), fingerprint);

        if (!leagueKeepsCurrentSeason) {
            leagues.save(leaguePreStructure.withCurrentSeason(target.season()));
        }
    }

    /**
     * The shared acceptance step. Runs at the end of both Applied and Unchanged, inside the same
     * transaction as any structural write. Upserts club display names, upserts the league's
     * display name without touching its current season, and invokes the correlation re-apply
     * slot so provider work can refresh references on both paths.
     */
    private void acceptanceStep(PreparedStructure input) {
        for (Club club : input.clubs()) {
            clubs.save(club);
        }
        leagues.updateDisplayName(input.league().id(), input.league().name());
        correlations.reApplyFor(input.structure().id());
    }
}
