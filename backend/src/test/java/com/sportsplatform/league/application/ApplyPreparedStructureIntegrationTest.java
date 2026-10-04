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

/**
 * End-to-end apply operation on PostgreSQL: the three outcomes, the current-season rule, and the
 * canonical-form fingerprint reconciliation.
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
        var outcome = apply.apply(preparedFixture("full"));

        assertThat(outcome).isInstanceOf(ApplyOutcome.Applied.class);
        var applied = (ApplyOutcome.Applied) outcome;
        assertThat(applied.target()).isEqualTo(TARGET);

        assertThat(leagues.find(LEAGUE_ID)).hasValueSatisfying(l ->
                assertThat(l.currentSeason()).contains(SEASON_ID));
        assertThat(clubRepository.find(new ClubId("c-1"))).isPresent();
        assertThat(structures.find(TARGET)).isPresent();
        assertThat(structures.findFingerprint(TARGET)).contains(applied.fingerprint());
        assertThat(countRefusals()).isZero();
    }

    @Test
    void unchanged_when_the_same_input_is_applied_again() {
        var first = apply.apply(preparedFixture("full"));
        assertThat(first).isInstanceOf(ApplyOutcome.Applied.class);

        var second = apply.apply(preparedFixture("full"));
        assertThat(second).isInstanceOf(ApplyOutcome.Unchanged.class);

        assertThat(countRefusals()).isZero();
    }

    @Test
    void refused_and_recorded_when_the_fingerprint_differs() {
        var first = apply.apply(preparedFixture("full"));
        assertThat(first).isInstanceOf(ApplyOutcome.Applied.class);

        var outcome = apply.apply(preparedFixture("altered"));
        assertThat(outcome).isInstanceOf(ApplyOutcome.RefusedAndRecorded.class);

        var refusal = (ApplyOutcome.RefusedAndRecorded) outcome;
        assertThat(refusal.storedFingerprint()).isNotEqualTo(refusal.incomingFingerprint());

        // The stored structure is untouched; the stored fingerprint still matches the first apply.
        assertThat(structures.findFingerprint(TARGET)).contains(refusal.storedFingerprint());
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
        apply.apply(preparedFixture("full"));

        var reordered = apply.apply(preparedFixtureReordered("full"));

        assertThat(reordered).isInstanceOf(ApplyOutcome.Unchanged.class);
        assertThat(countRefusals()).isZero();
    }

    @Test
    void current_season_is_not_moved_when_the_league_already_has_one() {
        // First application sets the current season.
        apply.apply(preparedFixture("full"));
        var afterFirst = leagues.find(LEAGUE_ID).orElseThrow();
        assertThat(afterFirst.currentSeason()).contains(SEASON_ID);

        // Insert a second season's structure for the same league by applying it; the current
        // season on the league must stay at the first season.
        var otherSeason = new SeasonId("2027-2028");
        var otherTarget = new SeasonStructureId(LEAGUE_ID, otherSeason);
        var otherStructure = emptyStructure(otherTarget);
        var otherLeague = new League(LEAGUE_ID, "NBA", null);
        apply.apply(new PreparedStructure(otherLeague, List.of(), otherStructure));

        var afterSecond = leagues.find(LEAGUE_ID).orElseThrow();
        assertThat(afterSecond.currentSeason())
                .as("current season must stay at the first-applied season")
                .contains(SEASON_ID);
    }

    @Test
    void current_season_is_set_when_the_league_has_none_before_apply() {
        // No league row exists before apply.
        assertThat(leagues.find(LEAGUE_ID)).isEmpty();

        apply.apply(preparedFixture("full"));

        assertThat(leagues.find(LEAGUE_ID))
                .hasValueSatisfying(l -> assertThat(l.currentSeason()).contains(SEASON_ID));
    }

    @Test
    void invalid_input_is_rejected_before_any_write() {
        // Participation references a club with no placement — SeasonStructure's constructor
        // throws InvalidSeasonStructure, reporting the violation. The apply use case never
        // reaches the write phase.
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
            assertThat(expected.violations())
                    .anyMatch(v -> v.contains("no placement"));
        }

        // No structure, no refusal, no league row was ever created.
        assertThat(leagues.find(LEAGUE_ID)).isEmpty();
        assertThat(structures.find(TARGET)).isEmpty();
        assertThat(countRefusals()).isZero();
    }

    private long countRefusals() {
        return jdbc.sql("select count(*) from refusal_record").query(Long.class).single();
    }

    // ---- fixtures ----

    private PreparedStructure preparedFixture(String variant) {
        return prepared(variant, false);
    }

    private PreparedStructure preparedFixtureReordered(String variant) {
        return prepared(variant, true);
    }

    private PreparedStructure prepared(String variant, boolean reordered) {
        var league = new League(LEAGUE_ID, "NBA", null);
        var clubs = List.of(
                new Club(new ClubId("c-1"), LEAGUE_ID, "Club 1"),
                new Club(new ClubId("c-2"), LEAGUE_ID, "Club 2"),
                new Club(new ClubId("c-3"), LEAGUE_ID, "Club 3"));

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

        var baseParticipations = List.of(
                CompetitionParticipation.inCompetition(new ClubId("c-1"), new CompetitionId("regular")),
                CompetitionParticipation.inCompetition(new ClubId("c-2"), new CompetitionId("regular")),
                CompetitionParticipation.inCompetition(new ClubId("c-3"), new CompetitionId("regular")),
                CompetitionParticipation.inCupGroup(new ClubId("c-1"), new CompetitionId("cup"), new CupGroupId("group-a")),
                CompetitionParticipation.inCupGroup(new ClubId("c-2"), new CompetitionId("cup"), new CupGroupId("group-b")));

        List<CompetitionParticipation> participations;
        if ("altered".equals(variant)) {
            // Move c-1 to a different Cup group — a real structural change that must refuse.
            participations = List.of(
                    CompetitionParticipation.inCompetition(new ClubId("c-1"), new CompetitionId("regular")),
                    CompetitionParticipation.inCompetition(new ClubId("c-2"), new CompetitionId("regular")),
                    CompetitionParticipation.inCompetition(new ClubId("c-3"), new CompetitionId("regular")),
                    CompetitionParticipation.inCupGroup(new ClubId("c-1"), new CompetitionId("cup"), new CupGroupId("group-b")),
                    CompetitionParticipation.inCupGroup(new ClubId("c-2"), new CompetitionId("cup"), new CupGroupId("group-a")));
        } else {
            participations = baseParticipations;
        }

        if (reordered) {
            // Keep the same facts but hand them to the constructor in a different order.
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

    private static SeasonStructure emptyStructure(SeasonStructureId id) {
        return new SeasonStructure(id, List.of(), List.of(), List.of());
    }
}
