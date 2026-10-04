package com.sportsplatform.league.domain;

import java.util.Optional;

public interface LeagueRepository {
    Optional<League> find(LeagueId id);
    void save(League league);
}
