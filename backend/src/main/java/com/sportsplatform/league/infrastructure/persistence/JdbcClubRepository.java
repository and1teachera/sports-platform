package com.sportsplatform.league.infrastructure.persistence;

import com.sportsplatform.league.domain.Club;
import com.sportsplatform.league.domain.ClubId;
import com.sportsplatform.league.domain.ClubRepository;
import com.sportsplatform.league.domain.LeagueId;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcClubRepository implements ClubRepository {

    private final JdbcClient jdbc;

    public JdbcClubRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Club> find(ClubId id) {
        return jdbc.sql("select id, league_id, name from club where id = ?")
                .param(id.value())
                .query((rs, rowNum) -> new Club(
                        new ClubId(rs.getString("id")),
                        new LeagueId(rs.getString("league_id")),
                        rs.getString("name")
                ))
                .optional();
    }

    @Override
    public void save(Club club) {
        int updated = jdbc.sql("update club set league_id = ?, name = ? where id = ?")
                .param(club.league().value())
                .param(club.name())
                .param(club.id().value())
                .update();
        if (updated == 0) {
            jdbc.sql("insert into club (id, league_id, name) values (?, ?, ?)")
                    .param(club.id().value())
                    .param(club.league().value())
                    .param(club.name())
                    .update();
        }
    }
}
