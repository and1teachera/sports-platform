package com.sportsplatform.league.domain;

import java.util.Optional;

public interface ClubRepository {
    Optional<Club> find(ClubId id);
    void save(Club club);
}
