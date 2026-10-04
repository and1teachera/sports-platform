package com.sportsplatform.league.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validation rules of the League context's domain types. No Spring context; records and plain Java.
 */
class LeagueDomainTypesTest {

    @Test
    void identifiers_reject_null_blank_and_untrimmed_values() {
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

    @Test
    void identifiers_accept_well_formed_values() {
        assertThat(new LeagueId("nba").value()).isEqualTo("nba");
        assertThat(new SeasonId("2026-2027").value()).isEqualTo("2026-2027");
        assertThat(new ClubId("lakers").value()).isEqualTo("lakers");
    }

    @Test
    void conference_and_division_reject_blank_names() {
        assertThatThrownBy(() -> new Conference(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Conference("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Conference(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Division(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Division("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void league_rejects_a_blank_name() {
        assertThatThrownBy(() -> new League(new LeagueId("nba"), "", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new League(new LeagueId("nba"), " ", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new League(new LeagueId("nba"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void club_rejects_a_blank_name() {
        assertThatThrownBy(() -> new Club(new ClubId("c"), new LeagueId("nba"), ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Club(new ClubId("c"), new LeagueId("nba"), " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Club(new ClubId("c"), new LeagueId("nba"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
