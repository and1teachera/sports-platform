package com.sportsplatform.league.domain;

/**
 * One Cup group inside a group-play phase.
 */
public record CupGroup(CupGroupId id, String name) {
    public CupGroup {
        java.util.Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("CupGroup name must not be blank");
        }
    }
}
