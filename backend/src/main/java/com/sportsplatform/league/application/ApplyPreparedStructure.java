package com.sportsplatform.league.application;

import com.sportsplatform.league.domain.Club;
import com.sportsplatform.league.domain.ClubRepository;
import com.sportsplatform.league.domain.Clock;
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

/**
 * Apply one prepared structure safely. Produces one of three outcomes:
 * {@link ApplyOutcome.Applied}, {@link ApplyOutcome.Unchanged} or
 * {@link ApplyOutcome.RefusedAndRecorded}.
 *
 * <p>The structure is written atomically across every table it occupies, or not at all. The
 * league's current season is set only when the league holds none; a refusal is recorded next to
 * the stored structure and does not halt the system.</p>
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

        return unitOfWork.execute(() -> decide(input, target, incoming));
    }

    private ApplyOutcome decide(PreparedStructure input, SeasonStructureId target, Fingerprint incoming) {
        return structures.findFingerprint(target)
                .map(stored -> stored.equals(incoming)
                        ? unchanged(target, stored)
                        : refuse(target, stored, incoming))
                .orElseGet(() -> applyFresh(input, target, incoming));
    }

    private ApplyOutcome applyFresh(PreparedStructure input, SeasonStructureId target, Fingerprint fingerprint) {
        // Write order honours the schema's composite foreign key from
        // league (id, current_season_id) to season_structure (league_id, season_id): the structure
        // must exist before a league row names its current season. The current season is set only
        // when the league held none; applying another season does not move an already-set current
        // season.
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

        correlations.reApplyFor(target);
        return new ApplyOutcome.Applied(target, fingerprint);
    }

    private ApplyOutcome unchanged(SeasonStructureId target, Fingerprint stored) {
        correlations.reApplyFor(target);
        return new ApplyOutcome.Unchanged(target, stored);
    }

    private ApplyOutcome refuse(SeasonStructureId target, Fingerprint stored, Fingerprint incoming) {
        refusals.save(new RefusalRecord(target, stored, incoming, clock.now()));
        return new ApplyOutcome.RefusedAndRecorded(target, stored, incoming);
    }
}
