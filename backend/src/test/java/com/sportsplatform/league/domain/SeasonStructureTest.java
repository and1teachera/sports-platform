package com.sportsplatform.league.domain;

import org.junit.jupiter.api.DisplayName;
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

    @DisplayName("A valid shuffled input is stored in canonical order")
    @Test
    void shouldStoreShuffledInputInCanonicalOrder() {
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

    @DisplayName("Invariant 1: duplicate competition ids are rejected")
    @Test
    void shouldRejectDuplicateCompetitionIds() {
        Competition one = new Competition(REGULAR, "A", List.of(new Phase(REG_PHASE, "P", 1, List.of())));
        Competition two = new Competition(REGULAR, "B", List.of(new Phase(new PhaseId("other"), "Q", 1, List.of())));
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(one, two), List.of(), List.of()))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("duplicate competition id");
    }

    @DisplayName("Invariant 1: a competition with no phases is rejected")
    @Test
    void shouldRejectCompetitionWithoutPhases() {
        Competition empty = new Competition(REGULAR, "A", List.of());
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(empty), List.of(), List.of()))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("has no phases");
    }

    @DisplayName("Invariant 2: duplicate phase ids are rejected")
    @Test
    void shouldRejectDuplicatePhaseIds() {
        Competition c = new Competition(REGULAR, "A", List.of(
                new Phase(REG_PHASE, "P", 1, List.of()),
                new Phase(REG_PHASE, "Q", 2, List.of())
        ));
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), List.of(), List.of()))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("duplicate phase id");
    }

    @DisplayName("Invariant 2: duplicate phase ordinals are rejected")
    @Test
    void shouldRejectDuplicatePhaseOrdinals() {
        Competition c = new Competition(REGULAR, "A", List.of(
                new Phase(REG_PHASE, "P", 1, List.of()),
                new Phase(new PhaseId("other"), "Q", 1, List.of())
        ));
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), List.of(), List.of()))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("duplicate phase ordinal");
    }

    @DisplayName("Invariant 3: duplicate Cup group ids within a competition are rejected")
    @Test
    void shouldRejectDuplicateCupGroupIds() {
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

    @DisplayName("Invariant 3: duplicate Cup group names within a competition are rejected")
    @Test
    void shouldRejectDuplicateCupGroupNames() {
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

    @DisplayName("Invariant 4: more than one placement per club is rejected")
    @Test
    void shouldRejectMultiplePlacementsPerClub() {
        Competition c = new Competition(REGULAR, "A", List.of(new Phase(REG_PHASE, "P", 1, List.of())));
        var placements = List.of(
                new SeasonPlacement(C1, EAST, ATL),
                new SeasonPlacement(C1, EAST, ATL)
        );
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), placements, List.of()))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("more than one placement");
    }

    @DisplayName("Invariant 5: participation in an unknown competition is rejected")
    @Test
    void shouldRejectParticipationInUnknownCompetition() {
        Competition c = new Competition(REGULAR, "A", List.of(new Phase(REG_PHASE, "P", 1, List.of())));
        var placements = List.of(new SeasonPlacement(C1, EAST, ATL));
        var participations = List.of(
                CompetitionParticipation.inCompetition(C1, new CompetitionId("nope"))
        );
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), placements, participations))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("unknown competition");
    }

    @DisplayName("Invariant 6: participation for a club without a placement is rejected")
    @Test
    void shouldRejectParticipationForUnplacedClub() {
        Competition c = new Competition(REGULAR, "A", List.of(new Phase(REG_PHASE, "P", 1, List.of())));
        var participations = List.of(CompetitionParticipation.inCompetition(C1, REGULAR));
        assertThatThrownBy(() -> new SeasonStructure(ID, List.of(c), List.of(), participations))
                .isInstanceOf(InvalidSeasonStructure.class)
                .hasMessageContaining("no placement");
    }

    @DisplayName("Invariant 7: two participations for the same club and competition are rejected")
    @Test
    void shouldRejectDuplicateParticipation() {
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

    @DisplayName("Invariant 8: a Cup group from another competition is rejected")
    @Test
    void shouldRejectCupGroupOfAnotherCompetition() {
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

    @DisplayName("Multiple violations are reported together")
    @Test
    void shouldReportMultipleViolationsTogether() {
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
