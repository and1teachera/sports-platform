package com.sportsplatform.config;

import com.sportsplatform.league.application.ListCurrentSeasonClubs;
import com.sportsplatform.league.application.SeasonClubQuery;
import com.sportsplatform.league.domain.LeagueId;
import com.sportsplatform.league.domain.LeagueRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the current-season clubs read against its ports and the configured default league.
 */
@Configuration
@EnableConfigurationProperties(LeagueProperties.class)
public class ListCurrentSeasonClubsConfiguration {

    @Bean
    public ListCurrentSeasonClubs listCurrentSeasonClubs(
            LeagueRepository leagues,
            SeasonClubQuery clubs,
            LeagueProperties properties
    ) {
        return new ListCurrentSeasonClubs(leagues, clubs, new LeagueId(properties.defaultId()));
    }
}
