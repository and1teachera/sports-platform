package com.sportsplatform.league.domain;

import java.util.List;
import java.util.Objects;

/**
 * One phase of a competition. A phase with groups is a group-play phase; a phase without groups
 * is any other phase. No kind field.
 */
public record Phase(PhaseId id, String name, int ordinal, List<CupGroup> groups) {
    public Phase {
        Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Phase name must not be blank");
        }
        if (ordinal <= 0) {
            throw new IllegalArgumentException("Phase ordinal must be positive, got " + ordinal);
        }
        Objects.requireNonNull(groups, "groups");
        groups = List.copyOf(groups);
    }
}
