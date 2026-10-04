package com.sportsplatform.league.application;

import java.time.Instant;

/**
 * Clock port used by the application layer to timestamp apply-side records. The adapter samples
 * the system clock in infrastructure; keeping the port in {@code league.application} prevents
 * domain code from reaching for the clock through its own package.
 */
public interface Clock {
    Instant now();
}
