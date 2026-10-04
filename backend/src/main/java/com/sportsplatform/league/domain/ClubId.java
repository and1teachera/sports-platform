package com.sportsplatform.league.domain;

/**
 * A club's lasting identity as the prepared data defines it. Holds across seasons; a rename
 * changes an attribute, not the identity. Not a provider's reference.
 */
public record ClubId(String value) {
    public ClubId {
        Identifiers.require(value, "ClubId");
    }
}
