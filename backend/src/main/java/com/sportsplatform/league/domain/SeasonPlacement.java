package com.sportsplatform.league.domain;

import java.util.Objects;

/**
 * One club's placement in a season: the conference and division the prepared data gives it. One
 * per club, held once per season regardless of how many competitions the club takes part in.
 */
public record SeasonPlacement(ClubId club, Conference conference, Division division) {
    public SeasonPlacement {
        Objects.requireNonNull(club, "club");
        Objects.requireNonNull(conference, "conference");
        Objects.requireNonNull(division, "division");
    }
}
