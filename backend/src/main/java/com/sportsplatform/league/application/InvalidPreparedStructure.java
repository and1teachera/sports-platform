package com.sportsplatform.league.application;

import java.util.List;
import java.util.Objects;

/**
 * Thrown when a {@link PreparedStructure} cannot be built because one or more whole-input checks
 * fail. All violations are gathered and reported together rather than failing on the first one.
 */
public final class InvalidPreparedStructure extends RuntimeException {

    private final List<String> violations;

    public InvalidPreparedStructure(List<String> violations) {
        super(composeMessage(violations));
        this.violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
    }

    public List<String> violations() {
        return violations;
    }

    private static String composeMessage(List<String> violations) {
        if (violations == null || violations.isEmpty()) {
            return "invalid prepared structure (no violations reported)";
        }
        if (violations.size() == 1) {
            return "invalid prepared structure: " + violations.get(0);
        }
        return "invalid prepared structure: " + violations.size() + " violations: " + String.join("; ", violations);
    }
}
