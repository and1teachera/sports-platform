package com.sportsplatform.league.application;

import com.sportsplatform.league.domain.ClubId;
import com.sportsplatform.league.domain.Competition;
import com.sportsplatform.league.domain.CompetitionId;
import com.sportsplatform.league.domain.CompetitionParticipation;
import com.sportsplatform.league.domain.Conference;
import com.sportsplatform.league.domain.CupGroup;
import com.sportsplatform.league.domain.CupGroupId;
import com.sportsplatform.league.domain.Division;
import com.sportsplatform.league.domain.Fingerprint;
import com.sportsplatform.league.domain.LeagueId;
import com.sportsplatform.league.domain.Phase;
import com.sportsplatform.league.domain.PhaseId;
import com.sportsplatform.league.domain.SeasonId;
import com.sportsplatform.league.domain.SeasonPlacement;
import com.sportsplatform.league.domain.SeasonStructure;
import com.sportsplatform.league.domain.SeasonStructureId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Canonical form: two inputs carrying the same facts in different orders produce the same
 * fingerprint. Finality: changing a structural fact changes the fingerprint; changing a club's
 * display name (not a structural fact) does not.
 */
class FingerprintTest {

    private static final LeagueId LEAGUE = new LeagueId("nba");
    private static final SeasonId SEASON = new SeasonId("2026-2027");
    private static final SeasonStructureId ID = new SeasonStructureId(LEAGUE, SEASON);

    @Test
    void same_facts_in_different_input_order_produce_the_same_fingerprint() {
        var structureA = fixture(ordering.A);
        var structureB = fixture(ordering.B);

        Fingerprint a = Fingerprint.of(structureA);
        Fingerprint b = Fingerprint.of(structureB);

        assertThat(a).isEqualTo(b);
    }

    @Test
    void a_different_participation_changes_the_fingerprint() {
        var baseline = fixture(ordering.A);

        var withExtraParticipation = new SeasonStructure(
                baseline.id(),
                baseline.competitions(),
                baseline.placements(),
                append(baseline.participations(),
                        CompetitionParticipation.inCompetition(new ClubId("c-3"), new CompetitionId("regular"))));

        // The baseline fixture only has c-1 and c-2 participating in regular; adding c-3 (which
        // does have a placement) changes a structural fact.
        assertThat(Fingerprint.of(baseline)).isNotEqualTo(Fingerprint.of(withExtraParticipation));
    }

    @Test
    void fingerprint_hex_is_lowercase_and_sixty_four_characters_long() {
        Fingerprint fp = Fingerprint.of(fixture(ordering.A));
        assertThat(fp.value()).matches("[0-9a-f]{64}");
    }

    private enum ordering { A, B }

    private static SeasonStructure fixture(ordering order) {
        var regularPhase = new Phase(new PhaseId("regular-phase"), "Regular", 1, List.of());
        var cupGroupPhase = new Phase(new PhaseId("cup-group"), "Groups", 1, List.of(
                new CupGroup(new CupGroupId("group-a"), "Group A"),
                new CupGroup(new CupGroupId("group-b"), "Group B")));
        var cupKnockoutPhase = new Phase(new PhaseId("cup-knockout"), "Knockout", 2, List.of());

        var regular = new Competition(new CompetitionId("regular"), "Regular Season", List.of(regularPhase));
        var cup = new Competition(new CompetitionId("cup"), "Cup",
                order == ordering.A ? List.of(cupGroupPhase, cupKnockoutPhase)
                                    : List.of(cupKnockoutPhase, cupGroupPhase));

        var placements = order == ordering.A
                ? List.of(
                        new SeasonPlacement(new ClubId("c-1"), new Conference("East"), new Division("Atlantic")),
                        new SeasonPlacement(new ClubId("c-2"), new Conference("East"), new Division("Atlantic")),
                        new SeasonPlacement(new ClubId("c-3"), new Conference("West"), new Division("Pacific")))
                : List.of(
                        new SeasonPlacement(new ClubId("c-3"), new Conference("West"), new Division("Pacific")),
                        new SeasonPlacement(new ClubId("c-1"), new Conference("East"), new Division("Atlantic")),
                        new SeasonPlacement(new ClubId("c-2"), new Conference("East"), new Division("Atlantic")));

        var participations = order == ordering.A
                ? List.of(
                        CompetitionParticipation.inCompetition(new ClubId("c-1"), new CompetitionId("regular")),
                        CompetitionParticipation.inCompetition(new ClubId("c-2"), new CompetitionId("regular")),
                        CompetitionParticipation.inCupGroup(new ClubId("c-1"), new CompetitionId("cup"), new CupGroupId("group-a")),
                        CompetitionParticipation.inCupGroup(new ClubId("c-2"), new CompetitionId("cup"), new CupGroupId("group-b")))
                : List.of(
                        CompetitionParticipation.inCupGroup(new ClubId("c-2"), new CompetitionId("cup"), new CupGroupId("group-b")),
                        CompetitionParticipation.inCompetition(new ClubId("c-2"), new CompetitionId("regular")),
                        CompetitionParticipation.inCupGroup(new ClubId("c-1"), new CompetitionId("cup"), new CupGroupId("group-a")),
                        CompetitionParticipation.inCompetition(new ClubId("c-1"), new CompetitionId("regular")));

        return new SeasonStructure(ID,
                order == ordering.A ? List.of(regular, cup) : List.of(cup, regular),
                placements, participations);
    }

    private static <T> List<T> append(List<T> list, T item) {
        var out = new java.util.ArrayList<>(list);
        out.add(item);
        return out;
    }
}
