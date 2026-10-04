package com.sportsplatform.league.domain;

import java.time.Instant;

/**
 * Domain-owned clock port. The adapter samples the system clock; application code reads the
 * instant through this port so no league code outside infrastructure asks the system for the
 * current time directly.
 */
public interface Clock {
    Instant now();
}
