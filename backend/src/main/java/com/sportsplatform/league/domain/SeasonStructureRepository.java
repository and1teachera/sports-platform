package com.sportsplatform.league.domain;

import java.util.Optional;

public interface SeasonStructureRepository {
    Optional<SeasonStructure> find(SeasonStructureId id);

    /**
     * Insert the structure. Fails if a structure for the same identity already exists.
     */
    void save(SeasonStructure structure);
}
