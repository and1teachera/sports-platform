package com.sportsplatform;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

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
 * The production migration set is deliberately empty at this checkpoint.
 */
@SpringBootTest
class BootstrapSmokeTest {

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("sports_platform_test")
                .withUsername("test")
                .withPassword("test");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @Autowired private Flyway flyway;

    @Test
    void the_spring_context_starts() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    void postgresql_is_reachable_through_the_autoconfigured_datasource() {
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(one).isEqualTo(1);
    }

    @Test
    void flyway_is_configured_through_spring_boot_and_its_startup_runs_cleanly() {
        assertThat(flyway).isNotNull();
        assertThat(flyway.getConfiguration().getDataSource()).isSameAs(dataSource);

        // Spring Boot's FlywayMigrationInitializer ran migrate() during application startup.
        // Re-run validate() here to assert the Flyway configuration is coherent against the
        // live database and the (empty) classpath migration set.
        flyway.validate();

        // Resolved and applied migration counts are both zero at this checkpoint; the point is
        // that Flyway inspected the migration set without throwing.
        assertThat(flyway.info().all()).isEmpty();
        assertThat(flyway.info().applied()).isEmpty();
    }
}
