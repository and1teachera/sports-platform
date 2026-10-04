package com.sportsplatform.config;

import com.sportsplatform.league.application.ApplyOutcome;
import com.sportsplatform.league.application.ApplyPreparedStructure;
import com.sportsplatform.league.infrastructure.seed.PreparedStructureLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * Wires the seed-apply startup switch. The {@link #seedApplyRunner} bean is only registered when
 * {@code sports-platform.seed.apply-on-startup} is {@code true}; in every other case the backend
 * starts without touching any seed.
 */
@Configuration
@EnableConfigurationProperties(SeedProperties.class)
public class SeedConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "sports-platform.seed", name = "apply-on-startup", havingValue = "true")
    public ApplicationRunner seedApplyRunner(
            SeedProperties properties,
            PreparedStructureLoader loader,
            ApplyPreparedStructure applyPreparedStructure,
            ResourceLoader resourceLoader
    ) {
        Logger log = LoggerFactory.getLogger("com.sportsplatform.config.SeedConfiguration.seedApplyRunner");
        return args -> {
            if (properties.getPath() == null || properties.getPath().isBlank()) {
                log.warn("seed apply-on-startup is enabled but no seed path is set; nothing to apply");
                return;
            }
            var prepared = loader.load(resourceLoader.getResource(properties.getPath()));
            ApplyOutcome outcome = applyPreparedStructure.apply(prepared);
            switch (outcome) {
                case ApplyOutcome.Applied a -> log.info(
                        "seed applied: league={}, season={}, fingerprint={}",
                        a.target().league().value(), a.target().season().value(), a.fingerprint().value());
                case ApplyOutcome.Unchanged u -> log.info(
                        "seed unchanged: league={}, season={}, fingerprint={}",
                        u.target().league().value(), u.target().season().value(), u.fingerprint().value());
                case ApplyOutcome.RefusedAndRecorded r -> log.warn(
                        "seed refused and recorded: league={}, season={}, stored={}, incoming={}",
                        r.target().league().value(), r.target().season().value(),
                        r.storedFingerprint().value(), r.incomingFingerprint().value());
            }
        };
    }
}
