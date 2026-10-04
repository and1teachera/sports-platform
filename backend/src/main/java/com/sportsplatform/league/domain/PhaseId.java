package com.sportsplatform.league.domain;

public record PhaseId(String value) {
    public PhaseId {
        Identifiers.require(value, "PhaseId");
    }
}
