package com.sportsplatform.league.domain;

/**
 * Prepared data; never an enum. The name comes from the seed.
 */
public record Division(String name) {
    public Division {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Division name must not be blank");
        }
    }
}
