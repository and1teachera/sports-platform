package com.sportsplatform.league.application;

import com.sportsplatform.league.domain.Fingerprint;
import com.sportsplatform.league.domain.SeasonStructureId;

/**
 * The three outcomes of applying a prepared structure.
 *
 * <p>Applied: the league held no structure for that season, so the structure is now stored across
 * every table it occupies, in one transaction, with the given fingerprint, and the league's
 * current season is set if it had none. Correlation re-apply is a slot called on both the Applied
 * and Unchanged paths; the default implementation at this point does nothing.</p>
 *
 * <p>Unchanged: a structure for that season already exists, and the incoming structural fingerprint
 * equals the stored one. Nothing in the structure is written; no refusal record is written.
 * Correlation re-apply is invoked.</p>
 *
 * <p>RefusedAndRecorded: a structure for that season already exists, and the incoming fingerprint
 * differs from the stored one. Nothing in the structure is written; a refusal record carrying both
 * fingerprints is persisted. The system does not throw: a refusal is a recorded fact, not a
 * failure that halts startup.</p>
 */
public sealed interface ApplyOutcome {

    record Applied(SeasonStructureId target, Fingerprint fingerprint) implements ApplyOutcome {}

    record Unchanged(SeasonStructureId target, Fingerprint fingerprint) implements ApplyOutcome {}

    record RefusedAndRecorded(
            SeasonStructureId target,
            Fingerprint storedFingerprint,
            Fingerprint incomingFingerprint
    ) implements ApplyOutcome {}
}
