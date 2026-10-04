package com.sportsplatform.league;

import com.sportsplatform.support.DatabaseCleanedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SQL-level guards: these write directly to the schema and prove it rejects wrong data
 * independently of the aggregate's checks.
 */
@SpringBootTest
class SchemaGuardsTest extends DatabaseCleanedIntegrationTest {

    @Autowired private JdbcClient jdbc;

    @Test
    void a_league_cannot_name_a_current_season_without_a_structure() {
        assertThatThrownBy(() -> jdbc.sql(
                        "insert into league (id, name, current_season_id) values (?, ?, ?)")
                .param("nba").param("NBA").param("2026-2027")
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void a_participation_requires_a_season_placement_for_its_club() {
        jdbc.sql("insert into league (id, name) values ('nba', 'NBA')").update();
        jdbc.sql("insert into season_structure (league_id, season_id, fingerprint) values ('nba', '2026-2027', 'test-fp')").update();
        jdbc.sql("insert into club (id, league_id, name) values ('c-1', 'nba', 'Club 1')").update();
        jdbc.sql("""
                insert into competition (league_id, season_id, id, name)
                values ('nba', '2026-2027', 'regular', 'Regular Season')
                """).update();

        // No season_placement for c-1 — participation must fail.
        assertThatThrownBy(() -> jdbc.sql("""
                        insert into competition_participation
                        (league_id, season_id, competition_id, club_id, cup_group_id)
                        values ('nba', '2026-2027', 'regular', 'c-1', null)
                        """)
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void a_cup_group_assignment_must_belong_to_the_participation_s_competition() {
        jdbc.sql("insert into league (id, name) values ('nba', 'NBA')").update();
        jdbc.sql("insert into season_structure (league_id, season_id, fingerprint) values ('nba', '2026-2027', 'test-fp')").update();
        jdbc.sql("insert into club (id, league_id, name) values ('c-1', 'nba', 'Club 1')").update();
        jdbc.sql("""
                insert into competition (league_id, season_id, id, name)
                values ('nba', '2026-2027', 'regular', 'Regular Season')
                """).update();
        jdbc.sql("""
                insert into competition (league_id, season_id, id, name)
                values ('nba', '2026-2027', 'cup', 'Cup')
                """).update();
        jdbc.sql("""
                insert into competition_phase (league_id, season_id, competition_id, id, name, ordinal)
                values ('nba', '2026-2027', 'cup', 'groups', 'Groups', 1)
                """).update();
        jdbc.sql("""
                insert into cup_group (league_id, season_id, competition_id, phase_id, id, name)
                values ('nba', '2026-2027', 'cup', 'groups', 'group-a', 'Group A')
                """).update();
        jdbc.sql("""
                insert into season_placement (league_id, season_id, club_id, conference, division)
                values ('nba', '2026-2027', 'c-1', 'East', 'Atlantic')
                """).update();

        // 'group-a' is a Cup group; assigning it on a 'regular' participation must fail.
        assertThatThrownBy(() -> jdbc.sql("""
                        insert into competition_participation
                        (league_id, season_id, competition_id, club_id, cup_group_id)
                        values ('nba', '2026-2027', 'regular', 'c-1', 'group-a')
                        """)
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
