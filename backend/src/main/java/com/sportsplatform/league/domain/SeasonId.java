package com.sportsplatform.league.domain;

/**
 * The identifier portion of a season. A season has no entity of its own; the season's identity
 * is the pair ({@link LeagueId}, SeasonId).
 */
public record SeasonId(String value) {
    public SeasonId {
        Identifiers.require(value, "SeasonId");
    }
}
