package com.sportsplatform.league.domain;

import java.util.List;
import java.util.Objects;

/**
 * Thrown when a {@link SeasonStructure} cannot be built because one or more invariants fail.
 * All violations are gathered and reported together rather than failing on the first one.
 */
public final class InvalidSeasonStructure extends RuntimeException {

    private final List<String> violations;

    public InvalidSeasonStructure(List<String> violations) {
        super(composeMessage(violations));
        this.violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
    }

    public List<String> violations() {
        return violations;
    }

    private static String composeMessage(List<String> violations) {
        if (violations == null || violations.isEmpty()) {
            return "invalid season structure (no violations reported)";
        }
        if (violations.size() == 1) {
            return "invalid season structure: " + violations.get(0);
        }
        return "invalid season structure: " + violations.size() + " violations: " + String.join("; ", violations);
    }
}
