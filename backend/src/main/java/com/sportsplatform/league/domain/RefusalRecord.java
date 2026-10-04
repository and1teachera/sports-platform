package com.sportsplatform.league.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A record of an application that was refused because the incoming structural fingerprint
 * differed from the stored one. The stored structure is left intact; the refusal is written as a
 * side effect of the apply operation so the operator has a signal.
 */
public record RefusalRecord(
        SeasonStructureId target,
        Fingerprint storedFingerprint,
        Fingerprint incomingFingerprint,
        Instant detectedAt
) {
    public RefusalRecord {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(storedFingerprint, "storedFingerprint");
        Objects.requireNonNull(incomingFingerprint, "incomingFingerprint");
        Objects.requireNonNull(detectedAt, "detectedAt");
    }
}
