package com.sportsplatform.league.domain;

import java.util.List;
import java.util.Objects;

/**
 * One competition in a season structure. Carries at least one phase, with phase ids and ordinals
 * unique inside this competition.
 */
public record Competition(CompetitionId id, String name, List<Phase> phases) {
    public Competition {
        Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Competition name must not be blank");
        }
        Objects.requireNonNull(phases, "phases");
        phases = List.copyOf(phases);
    }
}
