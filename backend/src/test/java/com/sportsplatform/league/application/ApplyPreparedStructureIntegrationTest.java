package com.sportsplatform.league.application;

import com.sportsplatform.league.domain.Club;
import com.sportsplatform.league.domain.ClubId;
import com.sportsplatform.league.domain.ClubRepository;
import com.sportsplatform.league.domain.Competition;
import com.sportsplatform.league.domain.CompetitionId;
import com.sportsplatform.league.domain.CompetitionParticipation;
import com.sportsplatform.league.domain.Conference;
import com.sportsplatform.league.domain.CupGroup;
import com.sportsplatform.league.domain.CupGroupId;
import com.sportsplatform.league.domain.Division;
import com.sportsplatform.league.domain.Fingerprint;
import com.sportsplatform.league.domain.League;
import com.sportsplatform.league.domain.LeagueId;
import com.sportsplatform.league.domain.LeagueRepository;
import com.sportsplatform.league.domain.Phase;
import com.sportsplatform.league.domain.PhaseId;
import com.sportsplatform.league.domain.SeasonId;
import com.sportsplatform.league.domain.SeasonPlacement;
import com.sportsplatform.league.domain.SeasonStructure;
import com.sportsplatform.league.domain.SeasonStructureId;
import com.sportsplatform.league.domain.SeasonStructureRepository;
import com.sportsplatform.support.DatabaseCleanedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end apply operation on PostgreSQL: the three outcomes, the current-season rule, the
 * canonical-form fingerprint reconciliation, the acceptance step's refresh of display names on
 * Unchanged, and the refused-path postconditions.
 */
@SpringBootTest
class ApplyPreparedStructureIntegrationTest extends DatabaseCleanedIntegrationTest {

    @Autowired private ApplyPreparedStructure apply;
    @Autowired private LeagueRepository leagues;
    @Autowired private ClubRepository clubRepository;
    @Autowired private SeasonStructureRepository structures;
    @Autowired private JdbcClient jdbc;

    private static final LeagueId LEAGUE_ID = new LeagueId("nba");
    private static final SeasonId SEASON_ID = new SeasonId("2026-2027");
    private static final SeasonStructureId TARGET = new SeasonStructureId(LEAGUE_ID, SEASON_ID);

    @Test
    void applied_writes_the_whole_structure_and_sets_the_current_season() {
        var outcome = apply.apply(prepared("baseline", NameSet.ORIGINAL));

        assertThat(outcome).isInstanceOf(ApplyOutcome.Applied.class);
        var applied = (ApplyOutcome.Applied) outcome;
        assertThat(applied.target()).isEqualTo(TARGET);

        assertThat(leagues.find(LEAGUE_ID)).hasValueSatisfying(l -> {
            assertThat(l.currentSeason()).contains(SEASON_ID);
            assertThat(l.name()).isEqualTo("NBA");
        });
        assertThat(clubRepository.find(new ClubId("c-1"))).hasValueSatisfying(c ->
                assertThat(c.name()).isEqualTo("Club 1"));
        assertThat(structures.find(TARGET)).isPresent();
        assertThat(structures.findFingerprint(TARGET)).contains(applied.fingerprint());
        assertThat(countRefusals()).isZero();
    }

    @Test
    void unchanged_when_the_same_input_is_applied_again() {
        apply.apply(prepared("baseline", NameSet.ORIGINAL));
        var second = apply.apply(prepared("baseline", NameSet.ORIGINAL));
        assertThat(second).isInstanceOf(ApplyOutcome.Unchanged.class);
        assertThat(countRefusals()).isZero();
    }

    @Test
    void refused_and_recorded_when_the_fingerprint_differs() {
        var first = apply.apply(prepared("baseline", NameSet.ORIGINAL));
        assertThat(first).isInstanceOf(ApplyOutcome.Applied.class);
        Fingerprint storedBeforeRefusal = structures.findFingerprint(TARGET).orElseThrow();

        // Alter structural participations AND rename clubs and league. On a refused path, none
        // of these must land: the stored structure stays as it was, the fingerprint does not
        // change, and the display names are not refreshed.
        var outcome = apply.apply(prepared("altered", NameSet.RENAMED));

        assertThat(outcome).isInstanceOf(ApplyOutcome.RefusedAndRecorded.class);
        var refusal = (ApplyOutcome.RefusedAndRecorded) outcome;
        assertThat(refusal.storedFingerprint()).isNotEqualTo(refusal.incomingFingerprint());

        // Postconditions on Refused: structure unchanged, stored fingerprint unchanged, display
        // names unchanged, refusal record carries both fingerprints.
        assertThat(structures.findFingerprint(TARGET)).contains(storedBeforeRefusal);
        assertThat(leagues.find(LEAGUE_ID).orElseThrow().name())
                .as("league display name must be unchanged on a refused apply")
                .isEqualTo("NBA");
        assertThat(clubRepository.find(new ClubId("c-1")).orElseThrow().name())
                .as("club display name must be unchanged on a refused apply")
                .isEqualTo("Club 1");

        assertThat(countRefusals()).isEqualTo(1);
        var row = jdbc.sql("""
                        select stored_fingerprint, incoming_fingerprint
                        from refusal_record order by id desc limit 1
                        """)
                .query((rs, rowNum) -> new String[]{
                        rs.getString("stored_fingerprint"), rs.getString("incoming_fingerprint")})
                .single();
        assertThat(row[0]).isEqualTo(refusal.storedFingerprint().value());
        assertThat(row[1]).isEqualTo(refusal.incomingFingerprint().value());
    }

    @Test
    void reordered_input_fingerprints_as_the_same_structure() {
        apply.apply(prepared("baseline", NameSet.ORIGINAL));
        var reordered = apply.apply(preparedReordered(NameSet.ORIGINAL));
        assertThat(reordered).isInstanceOf(ApplyOutcome.Unchanged.class);
        assertThat(countRefusals()).isZero();
    }

    @Test
    void renamed_club_and_league_on_unchanged_input_are_applied_to_their_rows_but_not_the_structure() {
        var first = apply.apply(prepared("baseline", NameSet.ORIGINAL));
        assertThat(first).isInstanceOf(ApplyOutcome.Applied.class);
        Fingerprint storedBefore = structures.findFingerprint(TARGET).orElseThrow();

        // Same structural facts, renamed clubs and renamed league.
        var outcome = apply.apply(prepared("baseline", NameSet.RENAMED));

        assertThat(outcome).isInstanceOf(ApplyOutcome.Unchanged.class);
        assertThat(structures.findFingerprint(TARGET)).contains(storedBefore);
        assertThat(countRefusals()).isZero();

        // Display names are refreshed.
        assertThat(leagues.find(LEAGUE_ID).orElseThrow().name()).isEqualTo("Renamed League");
        assertThat(clubRepository.find(new ClubId("c-1")).orElseThrow().name()).isEqualTo("Renamed Club 1");
        assertThat(clubRepository.find(new ClubId("c-2")).orElseThrow().name()).isEqualTo("Renamed Club 2");
        assertThat(clubRepository.find(new ClubId("c-3")).orElseThrow().name()).isEqualTo("Renamed Club 3");

        // League's current season is unchanged.
        assertThat(leagues.find(LEAGUE_ID).orElseThrow().currentSeason()).contains(SEASON_ID);
    }

    @Test
    void current_season_is_not_moved_when_the_league_already_has_one() {
        apply.apply(prepared("baseline", NameSet.ORIGINAL));
        assertThat(leagues.find(LEAGUE_ID).orElseThrow().currentSeason()).contains(SEASON_ID);

        var otherSeason = new SeasonId("2027-2028");
        var otherTarget = new SeasonStructureId(LEAGUE_ID, otherSeason);
        var otherStructure = new SeasonStructure(otherTarget, List.of(), List.of(), List.of());
        var otherLeague = new League(LEAGUE_ID, "NBA", null);
        apply.apply(new PreparedStructure(otherLeague, List.of(), otherStructure));

        assertThat(leagues.find(LEAGUE_ID).orElseThrow().currentSeason())
                .as("current season must stay at the first-applied season")
                .contains(SEASON_ID);
    }

    @Test
    void current_season_is_set_when_the_league_has_none_before_apply() {
        assertThat(leagues.find(LEAGUE_ID)).isEmpty();
        apply.apply(prepared("baseline", NameSet.ORIGINAL));
        assertThat(leagues.find(LEAGUE_ID))
                .hasValueSatisfying(l -> assertThat(l.currentSeason()).contains(SEASON_ID));
    }

    @Test
    void invalid_input_is_rejected_before_any_write_at_the_structure_level() {
        // Participation references a club with no placement — SeasonStructure's constructor
        // throws InvalidSeasonStructure. The apply use case never runs.
        try {
            new SeasonStructure(
                    TARGET,
                    List.of(new Competition(new CompetitionId("regular"), "Regular",
                            List.of(new Phase(new PhaseId("p"), "P", 1, List.of())))),
                    List.of(),
                    List.of(CompetitionParticipation.inCompetition(new ClubId("c-404"), new CompetitionId("regular")))
            );
            throw new AssertionError("expected InvalidSeasonStructure");
        } catch (com.sportsplatform.league.domain.InvalidSeasonStructure expected) {
            assertThat(expected.violations()).anyMatch(v -> v.contains("no placement"));
        }

        assertThat(leagues.find(LEAGUE_ID)).isEmpty();
        assertThat(structures.find(TARGET)).isEmpty();
        assertThat(countRefusals()).isZero();
    }

    @Test
    void invalid_prepared_structure_rejects_cross_check_violations_before_apply_runs() {
        // Build a valid-aggregate SeasonStructure then compose a PreparedStructure that violates
        // the cross-field checks: a placement names a club the input's club list omits, and the
        // included club names a different league.
        var structure = new SeasonStructure(
                TARGET,
                List.of(new Competition(new CompetitionId("regular"), "Regular",
                        List.of(new Phase(new PhaseId("p"), "P", 1, List.of())))),
                List.of(new SeasonPlacement(new ClubId("missing"), new Conference("East"), new Division("Atlantic"))),
                List.of()
        );
        var otherLeague = new LeagueId("other");
        assertThatThrownBy(() -> new PreparedStructure(
                new League(LEAGUE_ID, "NBA", null),
                List.of(new Club(new ClubId("c-x"), otherLeague, "Club X")),
                structure))
                .isInstanceOf(InvalidPreparedStructure.class)
                .satisfies(e -> {
                    var violations = ((InvalidPreparedStructure) e).violations();
                    assertThat(violations)
                            .anyMatch(v -> v.contains("is not among the input's clubs"))
                            .anyMatch(v -> v.contains("does not match the input's league"));
                });
    }

    private long countRefusals() {
        return jdbc.sql("select count(*) from refusal_record").query(Long.class).single();
    }

    // ---- fixtures ----

    private enum NameSet { ORIGINAL, RENAMED }

    private PreparedStructure prepared(String variant, NameSet names) {
        return build(variant, names, false);
    }

    private PreparedStructure preparedReordered(NameSet names) {
        return build("baseline", names, true);
    }

    private PreparedStructure build(String variant, NameSet names, boolean reordered) {
        var leagueName = names == NameSet.ORIGINAL ? "NBA" : "Renamed League";
        var league = new League(LEAGUE_ID, leagueName, null);

        var clubs = names == NameSet.ORIGINAL
                ? List.of(
                        new Club(new ClubId("c-1"), LEAGUE_ID, "Club 1"),
                        new Club(new ClubId("c-2"), LEAGUE_ID, "Club 2"),
                        new Club(new ClubId("c-3"), LEAGUE_ID, "Club 3"))
                : List.of(
                        new Club(new ClubId("c-1"), LEAGUE_ID, "Renamed Club 1"),
                        new Club(new ClubId("c-2"), LEAGUE_ID, "Renamed Club 2"),
                        new Club(new ClubId("c-3"), LEAGUE_ID, "Renamed Club 3"));

        var regularPhase = new Phase(new PhaseId("regular-phase"), "Regular", 1, List.of());
        var cupGroupPhase = new Phase(new PhaseId("cup-group"), "Groups", 1, List.of(
                new CupGroup(new CupGroupId("group-a"), "Group A"),
                new CupGroup(new CupGroupId("group-b"), "Group B")));
        var cupKnockoutPhase = new Phase(new PhaseId("cup-knockout"), "Knockout", 2, List.of());

        var regular = new Competition(new CompetitionId("regular"), "Regular Season", List.of(regularPhase));
        var cup = new Competition(new CompetitionId("cup"), "Cup", List.of(cupGroupPhase, cupKnockoutPhase));

        var placements = List.of(
                new SeasonPlacement(new ClubId("c-1"), new Conference("East"), new Division("Atlantic")),
                new SeasonPlacement(new ClubId("c-2"), new Conference("East"), new Division("Atlantic")),
                new SeasonPlacement(new ClubId("c-3"), new Conference("West"), new Division("Pacific")));

        List<CompetitionParticipation> participations;
        if ("altered".equals(variant)) {
            participations = List.of(
                    CompetitionParticipation.inCompetition(new ClubId("c-1"), new CompetitionId("regular")),
                    CompetitionParticipation.inCompetition(new ClubId("c-2"), new CompetitionId("regular")),
                    CompetitionParticipation.inCompetition(new ClubId("c-3"), new CompetitionId("regular")),
                    CompetitionParticipation.inCupGroup(new ClubId("c-1"), new CompetitionId("cup"), new CupGroupId("group-b")),
                    CompetitionParticipation.inCupGroup(new ClubId("c-2"), new CompetitionId("cup"), new CupGroupId("group-a")));
        } else {
            participations = List.of(
                    CompetitionParticipation.inCompetition(new ClubId("c-1"), new CompetitionId("regular")),
                    CompetitionParticipation.inCompetition(new ClubId("c-2"), new CompetitionId("regular")),
                    CompetitionParticipation.inCompetition(new ClubId("c-3"), new CompetitionId("regular")),
                    CompetitionParticipation.inCupGroup(new ClubId("c-1"), new CompetitionId("cup"), new CupGroupId("group-a")),
                    CompetitionParticipation.inCupGroup(new ClubId("c-2"), new CompetitionId("cup"), new CupGroupId("group-b")));
        }

        if (reordered) {
            var shuffled = new java.util.ArrayList<>(participations);
            java.util.Collections.reverse(shuffled);
            participations = shuffled;
        }

        var structure = new SeasonStructure(TARGET,
                reordered ? List.of(cup, regular) : List.of(regular, cup),
                placements,
                participations);

        return new PreparedStructure(league, clubs, structure);
    }
}
