package com.sportsplatform.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Base class for integration tests that write to any League table. Truncates every table in the
 * {@code public} schema (except Flyway's history) before each test, so test classes do not bleed
 * state into each other.
 */
public abstract class DatabaseCleanedIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanDatabase() {
        new DatabaseCleaner(jdbcClient).clean();
    }
}
