package com.sportsplatform.league.application;

import com.sportsplatform.league.domain.ClubId;
import com.sportsplatform.league.domain.Conference;
import com.sportsplatform.league.domain.Division;

import java.util.Objects;

/**
 * One club as a reader sees it in one season: its lasting identity, its name, and the conference
 * and division its season placement gives it. Competition participation contributes nothing here.
 */
public record SeasonClub(ClubId id, String name, Conference conference, Division division) {
    public SeasonClub {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(conference, "conference");
        Objects.requireNonNull(division, "division");
    }
}
