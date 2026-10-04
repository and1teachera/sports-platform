package com.sportsplatform.league.domain;

import java.util.Optional;

public interface LeagueRepository {
    Optional<League> find(LeagueId id);
    void save(League league);

    /**
     * Update the league's display name without touching its current season. Used by the apply
     * operation's acceptance step so a renamed league in a seed with unchanged structural facts
     * refreshes the display name without disturbing the schema-enforced
     * (id, current_season_id) coupling.
     */
    void updateDisplayName(LeagueId id, String name);
}
