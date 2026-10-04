package com.sportsplatform.league.application;

import com.sportsplatform.league.domain.Club;
import com.sportsplatform.league.domain.League;
import com.sportsplatform.league.domain.SeasonPlacement;
import com.sportsplatform.league.domain.SeasonStructure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One prepared structure ready to be applied: the league it pins, the clubs it names, and the
 * structure itself. The {@link SeasonStructure} has already run its own invariants in its
 * constructor; this record adds the whole-input checks that cross the structure and the club
 * list, so the write phase of the apply operation never has to run FK-level recovery.
 *
 * <p>Checks that run here, with all violations reported together through
 * {@link InvalidPreparedStructure}:
 * <ul>
 *     <li>the league identity on the record matches the structure's league;</li>
 *     <li>every club carries that same league id;</li>
 *     <li>every club named by a season placement is present in the input's club list.</li>
 * </ul></p>
 */
public record PreparedStructure(League league, List<Club> clubs, SeasonStructure structure) {

    public PreparedStructure {
        Objects.requireNonNull(league, "league");
        Objects.requireNonNull(clubs, "clubs");
        Objects.requireNonNull(structure, "structure");
        clubs = List.copyOf(clubs);

        List<String> violations = new ArrayList<>();

        if (!league.id().equals(structure.league())) {
            violations.add("league id " + league.id().value()
                    + " does not match structure's league " + structure.league().value());
        }

        Set<String> clubIdsInInput = new HashSet<>();
        for (Club club : clubs) {
            clubIdsInInput.add(club.id().value());
            if (!club.league().equals(league.id())) {
                violations.add("club " + club.id().value()
                        + " names league " + club.league().value()
                        + " which does not match the input's league " + league.id().value());
            }
        }

        for (SeasonPlacement placement : structure.placements()) {
            if (!clubIdsInInput.contains(placement.club().value())) {
                violations.add("season placement names club " + placement.club().value()
                        + " which is not among the input's clubs");
            }
        }

        if (!violations.isEmpty()) {
            throw new InvalidPreparedStructure(violations);
        }
    }
}
