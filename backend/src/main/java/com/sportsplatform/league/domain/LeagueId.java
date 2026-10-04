package com.sportsplatform.league.domain;

/**
 * A league's identity. Opaque text, assigned by the prepared data.
 */
public record LeagueId(String value) {
    public LeagueId {
        Identifiers.require(value, "LeagueId");
    }
}
