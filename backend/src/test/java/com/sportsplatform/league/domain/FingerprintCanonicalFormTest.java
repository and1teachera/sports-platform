package com.sportsplatform.league.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden-text pin on the fingerprint's canonical form. The canonical form is the one surface the
 * fingerprint's determinism rests on; changing its shape without a fingerprint-version decision
 * and a migration recomputing every stored fingerprint is forbidden. This test fails loudly if a
 * change slips through.
 */
class FingerprintCanonicalFormTest {

    @Test
    void canonical_form_of_a_minimal_fixture_matches_a_hard_coded_golden_string() {
        var id = new SeasonStructureId(new LeagueId("nba"), new SeasonId("2026-2027"));

        var cupGroupPhase = new Phase(new PhaseId("cup-group"), "Groups", 1, List.of(
                new CupGroup(new CupGroupId("group-a"), "Group A")));
        var regular = new Competition(new CompetitionId("regular"), "Regular Season",
                List.of(new Phase(new PhaseId("regular-phase"), "Regular", 1, List.of())));
        var cup = new Competition(new CompetitionId("cup"), "Cup", List.of(cupGroupPhase));

        var placements = List.of(
                new SeasonPlacement(new ClubId("c-1"), new Conference("East"), new Division("Atlantic")));
        var participations = List.of(
                CompetitionParticipation.inCompetition(new ClubId("c-1"), new CompetitionId("regular")),
                CompetitionParticipation.inCupGroup(new ClubId("c-1"), new CompetitionId("cup"), new CupGroupId("group-a")));

        var structure = new SeasonStructure(id, List.of(regular, cup), placements, participations);

        String expected = String.join("\n",
                "S|nba|2026-2027",
                "C|cup",
                "P|cup|1|cup-group",
                "G|cup|cup-group|group-a",
                "C|regular",
                "P|regular|1|regular-phase",
                "L|c-1|East|Atlantic",
                "X|cup|c-1|group-a",
                "X|regular|c-1|"
        );

        assertThat(Fingerprint.canonicalForm(structure))
                .as("canonical form is the one surface the fingerprint's determinism rests on; "
                        + "if this test fails without a fingerprint-version decision + migration, revert the change")
                .isEqualTo(expected);
    }
}
