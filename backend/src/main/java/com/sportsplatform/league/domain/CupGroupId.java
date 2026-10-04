package com.sportsplatform.league.domain;

public record CupGroupId(String value) {
    public CupGroupId {
        Identifiers.require(value, "CupGroupId");
    }
}
