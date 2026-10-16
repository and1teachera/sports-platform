package com.sportsplatform.league.infrastructure.persistence;

import com.sportsplatform.league.application.SeasonClub;
import com.sportsplatform.league.application.SeasonClubQuery;
import com.sportsplatform.league.domain.ClubId;
import com.sportsplatform.league.domain.Conference;
import com.sportsplatform.league.domain.Division;
import com.sportsplatform.league.domain.SeasonStructureId;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Reads a season's clubs from its placements. The placement key (league, season, club) gives one
 * row per club; competition participations are not joined, so a club in several competitions
 * still appears once.
 */
@Repository
public class JdbcSeasonClubQuery implements SeasonClubQuery {

    private final JdbcClient jdbc;

    public JdbcSeasonClubQuery(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<SeasonClub> placedIn(SeasonStructureId season) {
        return jdbc.sql("""
                        select c.id, c.name, p.conference, p.division
                        from season_placement p
                        join club c on c.league_id = p.league_id and c.id = p.club_id
                        where p.league_id = ? and p.season_id = ?
                        order by c.id
                        """)
                .param(season.league().value()).param(season.season().value())
                .query((rs, rowNum) -> new SeasonClub(
                        new ClubId(rs.getString("id")),
                        rs.getString("name"),
                        new Conference(rs.getString("conference")),
                        new Division(rs.getString("division"))))
                .list();
    }
}
