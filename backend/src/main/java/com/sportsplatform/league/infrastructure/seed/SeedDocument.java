package com.sportsplatform.league.infrastructure.seed;

import java.util.List;

/**
 * Wire shape of a prepared-structure seed file. Deserialised from YAML; mapped to the
 * application-layer input by {@link PreparedStructureLoader}. Lives in infrastructure, so the
 * domain and application layers do not know about the on-disk format.
 */
public record SeedDocument(
        LeagueSection league,
        SeasonSection season,
        List<ClubSection> clubs,
        List<CompetitionSection> competitions,
        List<PlacementSection> placements,
        List<ParticipationSection> participations
) {

    public record LeagueSection(String id, String name) {}

    public record SeasonSection(String id) {}

    public record ClubSection(String id, String name) {}

    public record CompetitionSection(String id, String name, List<PhaseSection> phases) {}

    public record PhaseSection(String id, String name, int ordinal, List<GroupSection> groups) {}

    public record GroupSection(String id, String name) {}

    public record PlacementSection(String club, String conference, String division) {}

    public record ParticipationSection(String club, String competition, String cupGroup) {}
}
