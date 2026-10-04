package com.sportsplatform.config;

import com.sportsplatform.league.application.ApplyPreparedStructure;
import com.sportsplatform.league.domain.ClubRepository;
import com.sportsplatform.league.domain.Clock;
import com.sportsplatform.league.domain.CorrelationReApplier;
import com.sportsplatform.league.domain.LeagueRepository;
import com.sportsplatform.league.domain.RefusalRecordStore;
import com.sportsplatform.league.domain.SeasonStructureRepository;
import com.sportsplatform.league.domain.UnitOfWork;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the apply use case against its ports. The configuration lives here so the application
 * layer stays Spring-free; the use case class it constructs is plain Java against domain ports.
 */
@Configuration
public class ApplyPreparedStructureConfiguration {

    @Bean
    public ApplyPreparedStructure applyPreparedStructure(
            LeagueRepository leagues,
            ClubRepository clubs,
            SeasonStructureRepository structures,
            RefusalRecordStore refusals,
            CorrelationReApplier correlations,
            UnitOfWork unitOfWork,
            Clock clock
    ) {
        return new ApplyPreparedStructure(leagues, clubs, structures, refusals, correlations, unitOfWork, clock);
    }
}
