package com.sportsplatform.league.application;

import com.sportsplatform.league.domain.Club;
import com.sportsplatform.league.domain.League;
import com.sportsplatform.league.domain.SeasonStructure;

import java.util.List;
import java.util.Objects;

/**
 * One prepared structure ready to be applied: the league it pins, the clubs it names, and the
 * structure itself. The {@link SeasonStructure} has already run its own invariants in its
 * constructor; the apply use case treats this value as validated input.
 */
public record PreparedStructure(League league, List<Club> clubs, SeasonStructure structure) {
    public PreparedStructure {
        Objects.requireNonNull(league, "league");
        Objects.requireNonNull(clubs, "clubs");
        Objects.requireNonNull(structure, "structure");
        clubs = List.copyOf(clubs);
        if (!league.id().equals(structure.league())) {
            throw new IllegalArgumentException(
                    "League id " + league.id() + " does not match structure's league "
                            + structure.league());
        }
    }
}
