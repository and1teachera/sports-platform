package com.sportsplatform.league.domain;

import java.util.Objects;

/**
 * Aggregate root for a club. Holds its lasting identity, the league it belongs to, and its name.
 * Carries no provider reference, no abbreviation, no succession link at this point.
 */
public final class Club {

    private final ClubId id;
    private final LeagueId league;
    private final String name;

    public Club(ClubId id, LeagueId league, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.league = Objects.requireNonNull(league, "league");
        this.name = requireName(name);
    }

    public ClubId id() {
        return id;
    }

    public LeagueId league() {
        return league;
    }

    public String name() {
        return name;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Club name must not be blank");
        }
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Club other)) return false;
        return id.equals(other.id) && league.equals(other.league) && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, league, name);
    }

    @Override
    public String toString() {
        return "Club[" + id + ", " + league + ", " + name + "]";
    }
}
