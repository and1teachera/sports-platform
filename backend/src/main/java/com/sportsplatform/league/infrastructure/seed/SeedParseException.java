package com.sportsplatform.league.infrastructure.seed;

/**
 * Thrown when a seed file cannot be parsed or mapped. The message names the offending field or
 * section so an operator can locate the problem in the seed.
 */
public class SeedParseException extends RuntimeException {
    public SeedParseException(String message) {
        super(message);
    }

    public SeedParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
