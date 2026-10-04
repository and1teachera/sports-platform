package com.sportsplatform.league.domain;

public record CompetitionId(String value) {
    public CompetitionId {
        Identifiers.require(value, "CompetitionId");
    }
}
