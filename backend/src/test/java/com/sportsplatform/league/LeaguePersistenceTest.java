package com.sportsplatform.league;

import com.sportsplatform.league.domain.Club;
import com.sportsplatform.league.domain.ClubId;
import com.sportsplatform.league.domain.ClubRepository;
import com.sportsplatform.league.domain.Competition;
import com.sportsplatform.league.domain.CompetitionId;
import com.sportsplatform.league.domain.CompetitionParticipation;
import com.sportsplatform.league.domain.Conference;
import com.sportsplatform.league.domain.CupGroup;
import com.sportsplatform.league.domain.CupGroupId;
import com.sportsplatform.league.domain.Division;
import com.sportsplatform.league.domain.League;
import com.sportsplatform.league.domain.LeagueId;
import com.sportsplatform.league.domain.LeagueRepository;
import com.sportsplatform.league.domain.Phase;
import com.sportsplatform.league.domain.PhaseId;
import com.sportsplatform.league.domain.SeasonId;
import com.sportsplatform.league.domain.SeasonPlacement;
import com.sportsplatform.league.domain.SeasonStructure;
import com.sportsplatform.league.domain.SeasonStructureId;
import com.sportsplatform.league.domain.SeasonStructureRepository;
import com.sportsplatform.support.DatabaseCleanedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end persistence over PostgreSQL 16: Flyway state after startup; League, Club and
 * SeasonStructure round trips; saving the same SeasonStructure identity twice fails.
 */
@SpringBootTest
class LeaguePersistenceTest extends DatabaseCleanedIntegrationTest {

    @Autowired private LeagueRepository leagues;
    @Autowired private ClubRepository clubs;
    @Autowired private SeasonStructureRepository structures;
    @Autowired private JdbcClient jdbc;

    @Test
    void v1_is_applied_cleanly() {
        Integer applied = jdbc.sql(
                        "select count(*) from flyway_schema_history where success = true and installed_rank = 1")
                .query(Integer.class).single();
        assertThat(applied).isEqualTo(1);
        Integer failed = jdbc.sql(
                        "select count(*) from flyway_schema_history where success = false")
                .query(Integer.class).single();
        assertThat(failed).isZero();
    }

    @Test
    void league_round_trip_without_and_then_with_a_current_season() {
        var leagueId = new LeagueId("nba");
        var seasonId = new SeasonId("2026-2027");

        leagues.save(League.withoutCurrentSeason(leagueId, "NBA"));
        var loaded = leagues.find(leagueId).orElseThrow();
        assertThat(loaded.name()).isEqualTo("NBA");
        assertThat(loaded.currentSeason()).isEmpty();

        structures.save(emptyStructure(leagueId, seasonId));
        leagues.save(loaded.withCurrentSeason(seasonId));

        var withCurrent = leagues.find(leagueId).orElseThrow();
        assertThat(withCurrent.currentSeason()).contains(seasonId);
    }

    @Test
    void club_round_trip() {
        var leagueId = new LeagueId("nba");
        leagues.save(League.withoutCurrentSeason(leagueId, "NBA"));

        var club = new Club(new ClubId("lakers"), leagueId, "Los Angeles Lakers");
        clubs.save(club);

        var loaded = clubs.find(new ClubId("lakers")).orElseThrow();
        assertThat(loaded).isEqualTo(club);
    }

    @Test
    void season_structure_round_trip_with_the_fixture_of_the_canonicalisation_test() {
        var leagueId = new LeagueId("nba");
        var seasonId = new SeasonId("2026-2027");
        var structureId = new SeasonStructureId(leagueId, seasonId);

        leagues.save(League.withoutCurrentSeason(leagueId, "NBA"));
        clubs.save(new Club(new ClubId("c-1"), leagueId, "Club 1"));
        clubs.save(new Club(new ClubId("c-2"), leagueId, "Club 2"));
        clubs.save(new Club(new ClubId("c-3"), leagueId, "Club 3"));

        var regularPhase = new Phase(new PhaseId("regular-phase"), "Regular Season", 1, List.of());
        var cupGroupPhase = new Phase(new PhaseId("cup-group"), "Groups", 1, List.of(
                new CupGroup(new CupGroupId("group-a"), "Group A"),
                new CupGroup(new CupGroupId("group-b"), "Group B")));
        var cupKnockoutPhase = new Phase(new PhaseId("cup-knockout"), "Knockout", 2, List.of());

        var regular = new Competition(new CompetitionId("regular"), "Regular Season", List.of(regularPhase));
        var cup = new Competition(new CompetitionId("cup"), "Cup", List.of(cupGroupPhase, cupKnockoutPhase));

        var placements = List.of(
                new SeasonPlacement(new ClubId("c-1"), new Conference("East"), new Division("Atlantic")),
                new SeasonPlacement(new ClubId("c-2"), new Conference("East"), new Division("Atlantic")),
                new SeasonPlacement(new ClubId("c-3"), new Conference("East"), new Division("Atlantic"))
        );
        var participations = List.of(
                CompetitionParticipation.inCompetition(new ClubId("c-1"), new CompetitionId("regular")),
                CompetitionParticipation.inCompetition(new ClubId("c-2"), new CompetitionId("regular")),
                CompetitionParticipation.inCompetition(new ClubId("c-3"), new CompetitionId("regular")),
                CompetitionParticipation.inCupGroup(new ClubId("c-1"), new CompetitionId("cup"), new CupGroupId("group-a")),
                CompetitionParticipation.inCupGroup(new ClubId("c-2"), new CompetitionId("cup"), new CupGroupId("group-b"))
        );

        var saved = new SeasonStructure(structureId, List.of(regular, cup), placements, participations);
        structures.save(saved);

        var loaded = structures.find(structureId).orElseThrow();
        assertThat(loaded).isEqualTo(saved);
    }

    @Test
    void saving_the_same_season_structure_identity_twice_fails() {
        var leagueId = new LeagueId("nba");
        var seasonId = new SeasonId("2026-2027");
        leagues.save(League.withoutCurrentSeason(leagueId, "NBA"));

        structures.save(emptyStructure(leagueId, seasonId));

        assertThatThrownBy(() -> structures.save(emptyStructure(leagueId, seasonId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static SeasonStructure emptyStructure(LeagueId leagueId, SeasonId seasonId) {
        return new SeasonStructure(
                new SeasonStructureId(leagueId, seasonId),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
