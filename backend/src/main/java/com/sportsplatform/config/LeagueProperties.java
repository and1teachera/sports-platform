package com.sportsplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which league the platform serves by default. The league's current season is the league's own
 * state, so it is never configured here.
 *
 * @param defaultId the default league's identity, as the prepared data defines it
 */
@ConfigurationProperties(prefix = "sports-platform.league")
public record LeagueProperties(String defaultId) {
}
