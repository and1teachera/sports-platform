package com.sportsplatform.league.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Season structure constructor: canonical ordering, and each of the eight invariants that live
 * inside the aggregate.
 */
class SeasonStructureTest {

    private static final LeagueId LEAGUE = new LeagueId("nba");
    private static final SeasonId SEASON = new SeasonId("2026-2027");
    private static final SeasonStructureId ID = new SeasonStructureId(LEAGUE, SEASON);

    private static final CompetitionId REGULAR = new CompetitionId("regular");
    private static final CompetitionId CUP = new CompetitionId("cup");
    private static final PhaseId REG_PHASE = new PhaseId("regular-phase");
    private static final PhaseId CUP_GROUP_PHASE = new PhaseId("cup-group");
    private static final PhaseId CUP_KNOCKOUT_PHASE = new PhaseId("cup-knockout");
    private static final CupGroupId GROUP_A = new CupGroupId("group-a");
    private static final CupGroupId GROUP_B = new CupGroupId("group-b");

    private static final ClubId C1 = new ClubId("c-1");
    private static final ClubId C2 = new ClubId("c-2");
    private static final ClubId C3 = new ClubId("c-3");
    private static final Conference EAST = new Conference("East");
    private static final Division ATL = new Division("Atlantic");

    @Test
    void a_valid_shuffled_input_is_stored_in_canonical_order() {
        Phase regularPhase = new Phase(REG_PHASE, "Regular Season", 1, List.of());
        Phase groupPhase = new Phase(CUP_GROUP_PHASE, "Groups", 1, List.of(
                new CupGroup(GROUP_B, "Group B"),
                new CupGroup(GROUP_A, "Group A")
        ));
        Phase knockoutPhase = new Phase(CUP_KNOCKOUT_PHASE, "Knockout", 2, List.of());

        Competition regular = new Competition(REGULAR, "Regular Season", List.of(regularPhase));
        Competition cup = new Competition(CUP, "Cup", List.of(knockoutPhase, groupPhase));

        var placements = List.of(
                new SeasonPlacement(C3, EAST, ATL),
                new SeasonPlacement(C1, EAST, ATL),
                new SeasonPlacement(C2, EAST, ATL)
        );
        var participations = List.of(
                CompetitionParticipation.inCompetition(C3, REGULAR),
                CompetitionParticipation.inCupGroup(C1, CUP, GROUP_B),
                CompetitionParticipation.inCompetition(C1, REGULAR),
                CompetitionParticipation.inCompetition(C2, REGULAR),
                CompetitionParticipation.inCupGroup(C2, CUP, GROUP_A)
        );

        SeasonStructure structure = new SeasonStructure(ID, List.of(cup, regular), placements, participations);

        assertThat(structure.competitions())
                .extracting(c -> c.id().value())
                .containsExactly("cup", "regular");
        assertThat(structure.competitions().get(0).phases())
                .extracting(Phase::ordinal)
                .containsExactly(1, 2);
        assertThat(structure.competitions().get(0).phases().get(0).groups())
                .extracting(g -> g.id().value())
                .containsExactly("group-a", "group-b");
        assertThat(structure.placements())
                .extracting(p -> p.club().value())
                .containsExactly("c-1", "c-2", "c-3");
        assertThat(structure.participations())
                .extracting(p -> p.competition().value() + ":" + p.club().value())
                .containsExactly("cup:c-1", "cup:c-2", "regular:c-1", "regular:c-2", "regular:c-3");
    }

    @Test
    void inv1_rejects_duplicate_competition_ids() {
        Competition one = new Competition(REGULAR, "A", List.of(new Phase(REG_PHASE, "P", 1, List.of())));
        Competition two = new Competition(REGULAR, "B", List.of(new Phase(new PhaseId("other"), "Q", 1, List.of())));
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(one, two), List.of(), List.of()))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("duplicate competition id");
    }

    @Test
    void inv1_rejects_a_competition_with_no_phases() {
        Competition empty = new Competition(REGULAR, "A", List.of());
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(empty), List.of(), List.of()))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("has no phases");
    }

    @Test
    void inv2_rejects_duplicate_phase_ids() {
        Competition c = new Competition(REGULAR, "A", List.of(
                new Phase(REG_PHASE, "P", 1, List.of()),
                new Phase(REG_PHASE, "Q", 2, List.of())
        ));
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), List.of(), List.of()))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("duplicate phase id");
    }

    @Test
    void inv2_rejects_duplicate_phase_ordinals() {
        Competition c = new Competition(REGULAR, "A", List.of(
                new Phase(REG_PHASE, "P", 1, List.of()),
                new Phase(new PhaseId("other"), "Q", 1, List.of())
        ));
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), List.of(), List.of()))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("duplicate phase ordinal");
    }

    @Test
    void inv3_rejects_duplicate_cup_group_ids_within_a_competition() {
        Competition c = new Competition(CUP, "Cup", List.of(
                new Phase(CUP_GROUP_PHASE, "Groups", 1, List.of(
                        new CupGroup(GROUP_A, "Group A"),
                        new CupGroup(GROUP_A, "Group A duplicate")
                ))
        ));
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), List.of(), List.of()))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("duplicate cup group id");
    }

    @Test
    void inv3_rejects_duplicate_cup_group_names_within_a_competition() {
        Competition c = new Competition(CUP, "Cup", List.of(
                new Phase(CUP_GROUP_PHASE, "Groups", 1, List.of(
                        new CupGroup(GROUP_A, "Group A"),
                        new CupGroup(GROUP_B, "Group A")
                ))
        ));
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), List.of(), List.of()))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("duplicate cup group name");
    }

    @Test
    void inv4_rejects_more_than_one_placement_per_club() {
        Competition c = new Competition(REGULAR, "A", List.of(new Phase(REG_PHASE, "P", 1, List.of())));
        var placements = List.of(
                new SeasonPlacement(C1, EAST, ATL),
                new SeasonPlacement(C1, EAST, ATL)
        );
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), placements, List.of()))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("more than one placement");
    }

    @Test
    void inv5_rejects_participation_in_an_unknown_competition() {
        Competition c = new Competition(REGULAR, "A", List.of(new Phase(REG_PHASE, "P", 1, List.of())));
        var placements = List.of(new SeasonPlacement(C1, EAST, ATL));
        var participations = List.of(
                CompetitionParticipation.inCompetition(C1, new CompetitionId("nope"))
        );
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), placements, participations))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("unknown competition");
    }

    @Test
    void inv6_rejects_participation_for_a_club_without_a_placement() {
        Competition c = new Competition(REGULAR, "A", List.of(new Phase(REG_PHASE, "P", 1, List.of())));
        var participations = List.of(CompetitionParticipation.inCompetition(C1, REGULAR));
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), List.of(), participations))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("no placement");
    }

    @Test
    void inv7_rejects_two_participations_for_same_club_and_competition() {
        Competition c = new Competition(REGULAR, "A", List.of(new Phase(REG_PHASE, "P", 1, List.of())));
        var placements = List.of(new SeasonPlacement(C1, EAST, ATL));
        var participations = List.of(
                CompetitionParticipation.inCompetition(C1, REGULAR),
                CompetitionParticipation.inCompetition(C1, REGULAR)
        );
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), placements, participations))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("more than one participation");
    }

    @Test
    void inv8_rejects_cup_group_from_another_competition() {
        Competition regular = new Competition(REGULAR, "Regular", List.of(new Phase(REG_PHASE, "P", 1, List.of())));
        Competition cup = new Competition(CUP, "Cup", List.of(
                new Phase(CUP_GROUP_PHASE, "Groups", 1, List.of(new CupGroup(GROUP_A, "A")))
        ));
        var placements = List.of(new SeasonPlacement(C1, EAST, ATL));
        var participations = List.of(
                CompetitionParticipation.inCupGroup(C1, REGULAR, GROUP_A)
        );
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(regular, cup), placements, participations))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("not a group of that competition");
    }

    @Test
    void multiple_violations_are_reported_together() {
        Competition c = new Competition(REGULAR, "A", List.of(new Phase(REG_PHASE, "P", 1, List.of())));
        var placements = List.of(
                new SeasonPlacement(C1, EAST, ATL),
                new SeasonPlacement(C1, EAST, ATL)
        );
        var participations = List.of(
                CompetitionParticipation.inCompetition(C2, new CompetitionId("nope")),
                CompetitionParticipation.inCompetition(C3, REGULAR)
        );

        try {
            new SeasonStructure(ID, List.of(c), placements, participations);
            throw new AssertionError("expected InvalidSeasonStructure");
        } catch (InvalidSeasonStructure e) {
            assertThat(e.violations())
                    .anyMatch(v -> v.contains("more than one placement"))
                    .anyMatch(v -> v.contains("unknown competition"))
                    .anyMatch(v -> v.contains("no placement"));
            assertThat(e.violations().size()).isGreaterThanOrEqualTo(3);
        }
    }
}
