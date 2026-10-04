package com.sportsplatform.league.domain;

import java.util.Optional;

public interface SeasonStructureRepository {
    Optional<SeasonStructure> find(SeasonStructureId id);

    /**
     * Insert the structure together with its fingerprint, atomically. Fails if a structure for the
     * same identity already exists.
     */
    void save(SeasonStructure structure, Fingerprint fingerprint);

    /**
     * The fingerprint stored for this structure, if one has been written.
     */
    Optional<Fingerprint> findFingerprint(SeasonStructureId id);
}
