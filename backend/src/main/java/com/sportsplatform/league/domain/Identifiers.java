package com.sportsplatform.league.domain;

final class Identifiers {

    private Identifiers() {
    }

    static void require(String value, String kind) {
        if (value == null) {
            throw new IllegalArgumentException(kind + " value must not be null");
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException(kind + " value must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(kind + " value must not have leading or trailing whitespace");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(kind + " value must not be blank");
        }
    }
}
