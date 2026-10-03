package com.sportsplatform;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
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
 * The production migration set is deliberately empty.
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
        // live database and the (empty) classpath migration set.
        flyway.validate();

        // Resolved and applied migration counts are both zero; the point is
        // that Flyway inspected the migration set without throwing.
        assertThat(flyway.info().all()).isEmpty();
        assertThat(flyway.info().applied()).isEmpty();
    }
}
