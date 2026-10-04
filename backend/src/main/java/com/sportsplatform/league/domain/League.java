package com.sportsplatform.league.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root for a league. Holds the league's identity, name, and its current season, if any.
 */
public final class League {

    private final LeagueId id;
    private final String name;
    private final SeasonId currentSeason;

    public League(LeagueId id, String name, SeasonId currentSeason) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireName(name);
        this.currentSeason = currentSeason;
    }

    public static League withoutCurrentSeason(LeagueId id, String name) {
        return new League(id, name, null);
    }

    public LeagueId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Optional<SeasonId> currentSeason() {
        return Optional.ofNullable(currentSeason);
    }

    public League withCurrentSeason(SeasonId season) {
        Objects.requireNonNull(season, "season");
        return new League(id, name, season);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("League name must not be blank");
        }
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof League other)) return false;
        return id.equals(other.id)
                && name.equals(other.name)
                && Objects.equals(currentSeason, other.currentSeason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, currentSeason);
    }

    @Override
    public String toString() {
        return "League[" + id + ", " + name + ", current=" + Optional.ofNullable(currentSeason) + "]";
    }
}
