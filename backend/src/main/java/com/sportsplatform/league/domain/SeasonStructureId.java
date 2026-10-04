package com.sportsplatform.league.domain;

import java.util.Objects;

/**
 * A season structure's identity, the pair of its league and season.
 */
public record SeasonStructureId(LeagueId league, SeasonId season) {
    public SeasonStructureId {
        Objects.requireNonNull(league, "league");
        Objects.requireNonNull(season, "season");
    }
}
