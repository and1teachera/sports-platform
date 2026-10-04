package com.sportsplatform.league.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * A club's participation in one competition in a season, optionally placed inside a Cup group.
 * Carries no conference and no division; the season placement is the single source of those.
 */
public record CompetitionParticipation(
        ClubId club,
        CompetitionId competition,
        CupGroupId cupGroup
) {
    public CompetitionParticipation {
        Objects.requireNonNull(club, "club");
        Objects.requireNonNull(competition, "competition");
    }

    public static CompetitionParticipation inCompetition(ClubId club, CompetitionId competition) {
        return new CompetitionParticipation(club, competition, null);
    }

    public static CompetitionParticipation inCupGroup(ClubId club, CompetitionId competition, CupGroupId cupGroup) {
        Objects.requireNonNull(cupGroup, "cupGroup");
        return new CompetitionParticipation(club, competition, cupGroup);
    }

    public Optional<CupGroupId> cupGroupId() {
        return Optional.ofNullable(cupGroup);
    }
}
