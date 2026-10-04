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
 * Unchanged, and the refused-path postconditions. The three clubs the fixtures use (Boston
 * Celtics, Chicago Bulls, Golden State Warriors) match the YAML seed fixture's clubs, so a reader
 * of the two files sees the same seed exercised through two different mechanisms.
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

    private static final ClubId CELTICS = new ClubId("club-001");
    private static final ClubId BULLS = new ClubId("club-006");
    private static final ClubId WARRIORS = new ClubId("club-021");

    private static final String LEAGUE_FULL_NAME = "National Basketball Association";
    private static final String LEAGUE_SHORT_NAME = "NBA";
    private static final String CELTICS_FULL_NAME = "Boston Celtics";
    private static final String CELTICS_SHORT_NAME = "Celtics";
    private static final String BULLS_FULL_NAME = "Chicago Bulls";
    private static final String BULLS_SHORT_NAME = "Bulls";
    private static final String WARRIORS_FULL_NAME = "Golden State Warriors";
    private static final String WARRIORS_SHORT_NAME = "Warriors";

    private static final CompetitionId REGULAR_SEASON = new CompetitionId("regular-season-2026-2027");
    private static final CompetitionId NBA_CUP = new CompetitionId("nba-cup-2026");
    private static final CupGroupId EAST_GROUP_A = new CupGroupId("east-group-a");
    private static final CupGroupId EAST_GROUP_C = new CupGroupId("east-group-c");
    private static final CupGroupId WEST_GROUP_C = new CupGroupId("west-group-c");

    @Test
    void applied_writes_the_whole_structure_and_sets_the_current_season() {
        var outcome = apply.apply(prepared("baseline", NameSet.FULL));

        assertThat(outcome).isInstanceOf(ApplyOutcome.Applied.class);
        var applied = (ApplyOutcome.Applied) outcome;
        assertThat(applied.target()).isEqualTo(TARGET);

        assertThat(leagues.find(LEAGUE_ID)).hasValueSatisfying(l -> {
            assertThat(l.currentSeason()).contains(SEASON_ID);
            assertThat(l.name()).isEqualTo(LEAGUE_FULL_NAME);
        });
        assertThat(clubRepository.find(CELTICS)).hasValueSatisfying(c ->
                assertThat(c.name()).isEqualTo(CELTICS_FULL_NAME));
        assertThat(structures.find(TARGET)).isPresent();
        assertThat(structures.findFingerprint(TARGET)).contains(applied.fingerprint());
        assertThat(countRefusals()).isZero();
    }

    @Test
    void unchanged_when_the_same_input_is_applied_again() {
        apply.apply(prepared("baseline", NameSet.FULL));
        var second = apply.apply(prepared("baseline", NameSet.FULL));
        assertThat(second).isInstanceOf(ApplyOutcome.Unchanged.class);
        assertThat(countRefusals()).isZero();
    }

    @Test
    void refused_and_recorded_when_the_fingerprint_differs() {
        var first = apply.apply(prepared("baseline", NameSet.FULL));
        assertThat(first).isInstanceOf(ApplyOutcome.Applied.class);
        Fingerprint storedBeforeRefusal = structures.findFingerprint(TARGET).orElseThrow();

        // Alter structural participations AND use the short-form names. On a refused path, none
        // of these must land: the stored structure stays as it was, the fingerprint does not
        // change, and the display names are not refreshed.
        var outcome = apply.apply(prepared("altered", NameSet.SHORT));

        assertThat(outcome).isInstanceOf(ApplyOutcome.RefusedAndRecorded.class);
        var refusal = (ApplyOutcome.RefusedAndRecorded) outcome;
        assertThat(refusal.storedFingerprint()).isNotEqualTo(refusal.incomingFingerprint());

        // Postconditions on Refused: structure unchanged, stored fingerprint unchanged, display
        // names unchanged, refusal record carries both fingerprints.
        assertThat(structures.findFingerprint(TARGET)).contains(storedBeforeRefusal);
        assertThat(leagues.find(LEAGUE_ID).orElseThrow().name())
                .as("league display name must be unchanged on a refused apply")
                .isEqualTo(LEAGUE_FULL_NAME);
        assertThat(clubRepository.find(CELTICS).orElseThrow().name())
                .as("club display name must be unchanged on a refused apply")
                .isEqualTo(CELTICS_FULL_NAME);

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
        apply.apply(prepared("baseline", NameSet.FULL));
        var reordered = apply.apply(preparedReordered(NameSet.FULL));
        assertThat(reordered).isInstanceOf(ApplyOutcome.Unchanged.class);
        assertThat(countRefusals()).isZero();
    }

    @Test
    void renamed_club_and_league_on_unchanged_input_are_applied_to_their_rows_but_not_the_structure() {
        var first = apply.apply(prepared("baseline", NameSet.FULL));
        assertThat(first).isInstanceOf(ApplyOutcome.Applied.class);
        Fingerprint storedBefore = structures.findFingerprint(TARGET).orElseThrow();

        // Same structural facts; the second application carries the short-form variants of the
        // same real-world names (NBA is an abbreviation of the full form, and the club short
        // forms are commonly used in headlines and standings tables).
        var outcome = apply.apply(prepared("baseline", NameSet.SHORT));

        assertThat(outcome).isInstanceOf(ApplyOutcome.Unchanged.class);
        assertThat(structures.findFingerprint(TARGET)).contains(storedBefore);
        assertThat(countRefusals()).isZero();

        // Display names are refreshed to the short forms.
        assertThat(leagues.find(LEAGUE_ID).orElseThrow().name()).isEqualTo(LEAGUE_SHORT_NAME);
        assertThat(clubRepository.find(CELTICS).orElseThrow().name()).isEqualTo(CELTICS_SHORT_NAME);
        assertThat(clubRepository.find(BULLS).orElseThrow().name()).isEqualTo(BULLS_SHORT_NAME);
        assertThat(clubRepository.find(WARRIORS).orElseThrow().name()).isEqualTo(WARRIORS_SHORT_NAME);

        // League's current season is unchanged.
        assertThat(leagues.find(LEAGUE_ID).orElseThrow().currentSeason()).contains(SEASON_ID);
    }

    @Test
    void current_season_is_not_moved_when_the_league_already_has_one() {
        apply.apply(prepared("baseline", NameSet.FULL));
        assertThat(leagues.find(LEAGUE_ID).orElseThrow().currentSeason()).contains(SEASON_ID);

        var otherSeason = new SeasonId("2027-2028");
        var otherTarget = new SeasonStructureId(LEAGUE_ID, otherSeason);
        var otherStructure = new SeasonStructure(otherTarget, List.of(), List.of(), List.of());
        var otherLeague = new League(LEAGUE_ID, LEAGUE_FULL_NAME, null);
        apply.apply(new PreparedStructure(otherLeague, List.of(), otherStructure));

        assertThat(leagues.find(LEAGUE_ID).orElseThrow().currentSeason())
                .as("current season must stay at the first-applied season")
                .contains(SEASON_ID);
    }

    @Test
    void current_season_is_set_when_the_league_has_none_before_apply() {
        assertThat(leagues.find(LEAGUE_ID)).isEmpty();
        apply.apply(prepared("baseline", NameSet.FULL));
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
                    List.of(new Competition(REGULAR_SEASON, "Regular Season",
                            List.of(new Phase(new PhaseId("regular-season-2026-2027-phase-1"),
                                    "Regular Season", 1, List.of())))),
                    List.of(),
                    List.of(CompetitionParticipation.inCompetition(new ClubId("club-404"), REGULAR_SEASON))
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
        // the cross-field checks: a placement names a club the input's club list omits
        // (club-404), and the included club carries the wrong league id.
        var structure = new SeasonStructure(
                TARGET,
                List.of(new Competition(REGULAR_SEASON, "Regular Season",
                        List.of(new Phase(new PhaseId("regular-season-2026-2027-phase-1"),
                                "Regular Season", 1, List.of())))),
                List.of(new SeasonPlacement(new ClubId("club-404"),
                        new Conference("Eastern Conference"), new Division("Atlantic"))),
                List.of()
        );
        var differentLeague = new LeagueId("other-league");
        assertThatThrownBy(() -> new PreparedStructure(
                new League(LEAGUE_ID, LEAGUE_FULL_NAME, null),
                List.of(new Club(CELTICS, differentLeague, CELTICS_FULL_NAME)),
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

    private enum NameSet { FULL, SHORT }

    private PreparedStructure prepared(String variant, NameSet names) {
        return build(variant, names, false);
    }

    private PreparedStructure preparedReordered(NameSet names) {
        return build("baseline", names, true);
    }

    private PreparedStructure build(String variant, NameSet names, boolean reordered) {
        var leagueName = names == NameSet.FULL ? LEAGUE_FULL_NAME : LEAGUE_SHORT_NAME;
        var league = new League(LEAGUE_ID, leagueName, null);

        var clubs = names == NameSet.FULL
                ? List.of(
                        new Club(CELTICS, LEAGUE_ID, CELTICS_FULL_NAME),
                        new Club(BULLS, LEAGUE_ID, BULLS_FULL_NAME),
                        new Club(WARRIORS, LEAGUE_ID, WARRIORS_FULL_NAME))
                : List.of(
                        new Club(CELTICS, LEAGUE_ID, CELTICS_SHORT_NAME),
                        new Club(BULLS, LEAGUE_ID, BULLS_SHORT_NAME),
                        new Club(WARRIORS, LEAGUE_ID, WARRIORS_SHORT_NAME));

        var regularPhase = new Phase(
                new PhaseId("regular-season-2026-2027-phase-1"), "Regular Season", 1, List.of());
        var cupGroupPhase = new Phase(
                new PhaseId("nba-cup-2026-group-play"), "Group Play", 1, List.of(
                        new CupGroup(EAST_GROUP_A, "East Group A"),
                        new CupGroup(EAST_GROUP_C, "East Group C"),
                        new CupGroup(WEST_GROUP_C, "West Group C")));
        var cupKnockoutPhase = new Phase(
                new PhaseId("nba-cup-2026-knockout-rounds"), "Knockout Rounds", 2, List.of());

        var regular = new Competition(REGULAR_SEASON, "Regular Season", List.of(regularPhase));
        var cup = new Competition(NBA_CUP, "NBA Cup", List.of(cupGroupPhase, cupKnockoutPhase));

        var placements = List.of(
                new SeasonPlacement(CELTICS,
                        new Conference("Eastern Conference"), new Division("Atlantic")),
                new SeasonPlacement(BULLS,
                        new Conference("Eastern Conference"), new Division("Central")),
                new SeasonPlacement(WARRIORS,
                        new Conference("Western Conference"), new Division("Pacific")));

        List<CompetitionParticipation> participations;
        if ("altered".equals(variant)) {
            // Boston Celtics moves from East Group C to East Group A; every other participation
            // stays the same. One structural change is enough to make the fingerprint differ.
            participations = List.of(
                    CompetitionParticipation.inCompetition(CELTICS, REGULAR_SEASON),
                    CompetitionParticipation.inCompetition(BULLS, REGULAR_SEASON),
                    CompetitionParticipation.inCompetition(WARRIORS, REGULAR_SEASON),
                    CompetitionParticipation.inCupGroup(CELTICS, NBA_CUP, EAST_GROUP_A),
                    CompetitionParticipation.inCupGroup(BULLS, NBA_CUP, EAST_GROUP_C),
                    CompetitionParticipation.inCupGroup(WARRIORS, NBA_CUP, WEST_GROUP_C));
        } else {
            // Baseline: Boston Celtics and Chicago Bulls share East Group C (their real 2026
            // group); Golden State Warriors is in West Group C (their real 2026 group).
            participations = List.of(
                    CompetitionParticipation.inCompetition(CELTICS, REGULAR_SEASON),
                    CompetitionParticipation.inCompetition(BULLS, REGULAR_SEASON),
                    CompetitionParticipation.inCompetition(WARRIORS, REGULAR_SEASON),
                    CompetitionParticipation.inCupGroup(CELTICS, NBA_CUP, EAST_GROUP_C),
                    CompetitionParticipation.inCupGroup(BULLS, NBA_CUP, EAST_GROUP_C),
                    CompetitionParticipation.inCupGroup(WARRIORS, NBA_CUP, WEST_GROUP_C));
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
