package com.sportsplatform.league.application;

import com.sportsplatform.league.domain.League;
import com.sportsplatform.league.domain.LeagueId;
import com.sportsplatform.league.domain.LeagueRepository;
import com.sportsplatform.league.domain.SeasonStructureId;

import java.util.Objects;
import java.util.Optional;

/**
 * Lists the clubs of the default league's current season. Resolves the configured league, then the
 * current season that league holds, then that season's placements and their clubs. Reads only
 * the platform's own store; nothing here reaches a provider.
 *
 * <p>Empty when the league is unknown or holds no current season yet.</p>
 */
public class ListCurrentSeasonClubs {

    private final LeagueRepository leagues;
    private final SeasonClubQuery clubs;
    private final LeagueId defaultLeague;

    public ListCurrentSeasonClubs(LeagueRepository leagues, SeasonClubQuery clubs, LeagueId defaultLeague) {
        this.leagues = Objects.requireNonNull(leagues, "leagues");
        this.clubs = Objects.requireNonNull(clubs, "clubs");
        this.defaultLeague = Objects.requireNonNull(defaultLeague, "defaultLeague");
    }

    public Optional<SeasonClubs> list() {
        return leagues.find(defaultLeague)
                .flatMap(League::currentSeason)
                .map(season -> new SeasonStructureId(defaultLeague, season))
                .map(season -> new SeasonClubs(season, clubs.placedIn(season)));
    }
}
