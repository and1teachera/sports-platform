package com.sportsplatform.league.application;

import com.sportsplatform.league.domain.ClubId;
import com.sportsplatform.league.domain.Conference;
import com.sportsplatform.league.domain.Division;
import com.sportsplatform.league.domain.League;
import com.sportsplatform.league.domain.LeagueId;
import com.sportsplatform.league.domain.LeagueRepository;
import com.sportsplatform.league.domain.SeasonId;
import com.sportsplatform.league.domain.SeasonStructureId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolution from the configured default league to that league's current season, against
 * in-memory ports. The store holds two seasons for the default league, so only the league's own
 * current season can explain which one is listed.
 */
class ListCurrentSeasonClubsTest {

    private static final LeagueId NBA = new LeagueId("nba");
    private static final LeagueId UNKNOWN_LEAGUE = new LeagueId("wnba");
    private static final SeasonId CURRENT = new SeasonId("2026-2027");
    private static final SeasonId PREVIOUS = new SeasonId("2025-2026");

    private static final SeasonClub CELTICS = new SeasonClub(
            new ClubId("club-001"), "Boston Celtics",
            new Conference("Eastern Conference"), new Division("Atlantic"));
    private static final SeasonClub WARRIORS = new SeasonClub(
            new ClubId("club-021"), "Golden State Warriors",
            new Conference("Western Conference"), new Division("Pacific"));

    private final InMemoryLeagues leagues = new InMemoryLeagues();
    private final Map<SeasonStructureId, List<SeasonClub>> placed = new HashMap<>();
    private final SeasonClubQuery query = season -> placed.getOrDefault(season, List.of());

    @DisplayName("The default league's current season decides which season's clubs are listed")
    @Test
    void shouldResolveDefaultLeagueToItsCurrentSeason() {
        leagues.save(new League(NBA, "National Basketball Association", CURRENT));
        placed.put(new SeasonStructureId(NBA, CURRENT), List.of(CELTICS));
        placed.put(new SeasonStructureId(NBA, PREVIOUS), List.of(WARRIORS));

        var result = new ListCurrentSeasonClubs(leagues, query, NBA).list();

        assertThat(result).hasValueSatisfying(clubs -> {
            assertThat(clubs.season()).isEqualTo(new SeasonStructureId(NBA, CURRENT));
            assertThat(clubs.clubs()).containsExactly(CELTICS);
        });
    }

    @DisplayName("Moving the league's current season moves the listed season with it")
    @Test
    void shouldFollowTheLeaguesCurrentSeason() {
        leagues.save(new League(NBA, "National Basketball Association", PREVIOUS));
        placed.put(new SeasonStructureId(NBA, CURRENT), List.of(CELTICS));
        placed.put(new SeasonStructureId(NBA, PREVIOUS), List.of(WARRIORS));

        var result = new ListCurrentSeasonClubs(leagues, query, NBA).list();

        assertThat(result).hasValueSatisfying(clubs -> {
            assertThat(clubs.season()).isEqualTo(new SeasonStructureId(NBA, PREVIOUS));
            assertThat(clubs.clubs()).containsExactly(WARRIORS);
        });
    }

    @DisplayName("A default league without a current season lists nothing")
    @Test
    void shouldListNothingWithoutCurrentSeason() {
        leagues.save(League.withoutCurrentSeason(NBA, "National Basketball Association"));
        placed.put(new SeasonStructureId(NBA, CURRENT), List.of(CELTICS));

        assertThat(new ListCurrentSeasonClubs(leagues, query, NBA).list()).isEmpty();
    }

    @DisplayName("An unknown default league lists nothing")
    @Test
    void shouldListNothingForUnknownLeague() {
        leagues.save(new League(NBA, "National Basketball Association", CURRENT));
        placed.put(new SeasonStructureId(NBA, CURRENT), List.of(CELTICS));

        assertThat(new ListCurrentSeasonClubs(leagues, query, UNKNOWN_LEAGUE).list()).isEmpty();
    }

    private static final class InMemoryLeagues implements LeagueRepository {
        private final Map<LeagueId, League> byId = new HashMap<>();

        @Override
        public Optional<League> find(LeagueId id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public void save(League league) {
            byId.put(league.id(), league);
        }

        @Override
        public void updateDisplayName(LeagueId id, String name) {
            byId.computeIfPresent(id, (k, league) -> new League(id, name, league.currentSeason().orElse(null)));
        }
    }
}
