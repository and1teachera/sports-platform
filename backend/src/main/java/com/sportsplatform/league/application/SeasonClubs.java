package com.sportsplatform.league.application;

import com.sportsplatform.league.domain.SeasonStructureId;

import java.util.List;
import java.util.Objects;

/**
 * The clubs placed in one season, one entry per club, keyed by the season's identity.
 */
public record SeasonClubs(SeasonStructureId season, List<SeasonClub> clubs) {
    public SeasonClubs {
        Objects.requireNonNull(season, "season");
        clubs = List.copyOf(Objects.requireNonNull(clubs, "clubs"));
    }
}
