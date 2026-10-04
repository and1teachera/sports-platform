package com.sportsplatform.league.infrastructure.persistence;

import com.sportsplatform.league.domain.League;
import com.sportsplatform.league.domain.LeagueId;
import com.sportsplatform.league.domain.LeagueRepository;
import com.sportsplatform.league.domain.SeasonId;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcLeagueRepository implements LeagueRepository {

    private final JdbcClient jdbc;

    public JdbcLeagueRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<League> find(LeagueId id) {
        return jdbc.sql("select id, name, current_season_id from league where id = ?")
                .param(id.value())
                .query((rs, rowNum) -> {
                    String currentSeason = rs.getString("current_season_id");
                    return new League(
                            new LeagueId(rs.getString("id")),
                            rs.getString("name"),
                            currentSeason == null ? null : new SeasonId(currentSeason)
                    );
                })
                .optional();
    }

    @Override
    public void save(League league) {
        int updated = jdbc.sql("update league set name = ?, current_season_id = ? where id = ?")
                .param(league.name())
                .param(league.currentSeason().map(SeasonId::value).orElse(null))
                .param(league.id().value())
                .update();
        if (updated == 0) {
            jdbc.sql("insert into league (id, name, current_season_id) values (?, ?, ?)")
                    .param(league.id().value())
                    .param(league.name())
                    .param(league.currentSeason().map(SeasonId::value).orElse(null))
                    .update();
        }
    }

    @Override
    public void updateDisplayName(LeagueId id, String name) {
        jdbc.sql("update league set name = ? where id = ?")
                .param(name)
                .param(id.value())
                .update();
    }
}
