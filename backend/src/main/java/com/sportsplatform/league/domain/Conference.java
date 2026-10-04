package com.sportsplatform.league.domain;

/**
 * Prepared data; never an enum. The name comes from the seed.
 */
public record Conference(String name) {
    public Conference {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Conference name must not be blank");
        }
    }
}
