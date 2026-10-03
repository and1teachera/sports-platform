package com.sportsplatform.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared superclass for integration tests. Starts a single PostgreSQL 16 container once per JVM
 * and points every subclass's Spring context at it, so test classes share the database and the
 * Spring context cache stays effective.
 *
 * <p>The container is intentionally not stopped; Testcontainers' Ryuk reaps it at JVM exit.</p>
 */
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("sports_platform_test")
                .withUsername("test")
                .withPassword("test");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
