package com.sportsplatform;

import com.sportsplatform.support.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Backend bootstrap smoke test. Proves three independent facts:
 * <ol>
 *     <li>the Spring Boot application context starts;</li>
 *     <li>PostgreSQL 16 is reachable through the autoconfigured datasource;</li>
 *     <li>Flyway participates in Spring Boot's startup lifecycle: the auto-configured
 *         {@link Flyway} bean is present, points at the application's datasource, and its
 *         startup validation runs cleanly against the empty migration set.</li>
 * </ol>
 * The production migration set is deliberately empty.
 */
@SpringBootTest
class BootstrapSmokeTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @Autowired private Flyway flyway;

    @DisplayName("The Spring context starts")
    @Test
    void shouldStartSpringContext() {
        assertThat(dataSource).isNotNull();
    }

    @DisplayName("PostgreSQL is reachable through the configured datasource")
    @Test
    void shouldConnectToPostgres() {
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(one).isEqualTo(1);
    }

    @DisplayName("Flyway is configured through Spring Boot and its startup runs cleanly")
    @Test
    void shouldRunFlywayAtStartup() {
        assertThat(flyway).isNotNull();
        assertThat(flyway.getConfiguration().getDataSource()).isSameAs(dataSource);

        // Spring Boot's FlywayMigrationInitializer ran migrate() during application startup.
        // Re-run validate() here to assert the Flyway configuration is coherent against the
        // live database and the classpath migration set.
        flyway.validate();

        // The migration set is non-empty from V1 onwards; Flyway resolved and applied the
        // committed migrations without throwing.
        assertThat(flyway.info().all()).isNotEmpty();
        assertThat(flyway.info().applied()).isNotEmpty();
    }
}
