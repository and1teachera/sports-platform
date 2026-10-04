package com.sportsplatform.league;

import com.sportsplatform.support.DatabaseCleanedIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The database cleaner wipes the League tables before each test, so a row written in one test
 * cannot leak into the next.
 */
@SpringBootTest
class DatabaseCleanerTest extends DatabaseCleanedIntegrationTest {

    @Autowired private JdbcClient jdbc;

    @DisplayName("The first test writes a row")
    @Test
    void shouldWriteRowInFirstTest() {
        jdbc.sql("insert into league (id, name) values ('nba', 'NBA')").update();
        Integer rows = jdbc.sql("select count(*) from league").query(Integer.class).single();
        assertThat(rows).isEqualTo(1);
    }

    @DisplayName("The second test starts with the table empty")
    @Test
    void shouldStartSecondTestWithEmptyTable() {
        Integer rows = jdbc.sql("select count(*) from league").query(Integer.class).single();
        assertThat(rows).isZero();
    }
}
