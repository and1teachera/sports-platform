package com.sportsplatform.league.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validation rules of the League context's domain types. No Spring context; records and plain Java.
 */
class LeagueDomainTypesTest {

    @DisplayName("Identifiers reject null, blank and untrimmed values")
    @Test
    void shouldRejectNullBlankAndUntrimmedIdentifiers() {
        assertThatThrownBy(() -> new LeagueId(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LeagueId("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LeagueId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LeagueId(" nba")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LeagueId("nba ")).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new SeasonId(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeasonId("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClubId("\t")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompetitionId(" a ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PhaseId(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CupGroupId("")).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Identifiers accept well-formed values")
    @Test
    void shouldAcceptWellFormedIdentifiers() {
        assertThat(new LeagueId("nba").value()).isEqualTo("nba");
        assertThat(new SeasonId("2026-2027").value()).isEqualTo("2026-2027");
        assertThat(new ClubId("lakers").value()).isEqualTo("lakers");
    }

    @DisplayName("Conference and division reject blank names")
    @Test
    void shouldRejectBlankConferenceAndDivisionNames() {
        assertThatThrownBy(() -> new Conference(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Conference("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Conference(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Division(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Division("")).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("A league rejects a blank name")
    @Test
    void shouldRejectBlankLeagueName() {
        assertThatThrownBy(() -> new League(new LeagueId("nba"), "", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new League(new LeagueId("nba"), " ", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new League(new LeagueId("nba"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("A club rejects a blank name")
    @Test
    void shouldRejectBlankClubName() {
        assertThatThrownBy(() -> new Club(new ClubId("c"), new LeagueId("nba"), ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Club(new ClubId("c"), new LeagueId("nba"), " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Club(new ClubId("c"), new LeagueId("nba"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
