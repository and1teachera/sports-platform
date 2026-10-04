package com.sportsplatform.league.infrastructure.persistence;

import com.sportsplatform.league.domain.ClubId;
import com.sportsplatform.league.domain.Competition;
import com.sportsplatform.league.domain.CompetitionId;
import com.sportsplatform.league.domain.CompetitionParticipation;
import com.sportsplatform.league.domain.Conference;
import com.sportsplatform.league.domain.CupGroup;
import com.sportsplatform.league.domain.CupGroupId;
import com.sportsplatform.league.domain.Division;
import com.sportsplatform.league.domain.Fingerprint;
import com.sportsplatform.league.domain.LeagueId;
import com.sportsplatform.league.domain.Phase;
import com.sportsplatform.league.domain.PhaseId;
import com.sportsplatform.league.domain.SeasonId;
import com.sportsplatform.league.domain.SeasonPlacement;
import com.sportsplatform.league.domain.SeasonStructure;
import com.sportsplatform.league.domain.SeasonStructureId;
import com.sportsplatform.league.domain.SeasonStructureRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcSeasonStructureRepository implements SeasonStructureRepository {

    private final JdbcClient jdbc;

    public JdbcSeasonStructureRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SeasonStructure> find(SeasonStructureId id) {
        String leagueId = id.league().value();
        String seasonId = id.season().value();

        boolean exists = jdbc.sql(
                        "select 1 from season_structure where league_id = ? and season_id = ?")
                .param(leagueId).param(seasonId)
                .query(Integer.class).optional().isPresent();
        if (!exists) return Optional.empty();

        var competitions = loadCompetitions(leagueId, seasonId);
        var placements = loadPlacements(leagueId, seasonId);
        var participations = loadParticipations(leagueId, seasonId);
        return Optional.of(new SeasonStructure(id, competitions, placements, participations));
    }

    @Override
    @Transactional
    public void save(SeasonStructure structure, Fingerprint fingerprint) {
        insertStructureRows(structure, java.util.Objects.requireNonNull(fingerprint, "fingerprint"));
    }

    @Override
    public Optional<Fingerprint> findFingerprint(SeasonStructureId id) {
        return jdbc.sql(
                        "select fingerprint from season_structure where league_id = ? and season_id = ?")
                .param(id.league().value()).param(id.season().value())
                .query((rs, rowNum) -> {
                    String value = rs.getString("fingerprint");
                    return value == null ? null : new Fingerprint(value);
                })
                .optional()
                .flatMap(Optional::ofNullable);
    }

    private void insertStructureRows(SeasonStructure structure, Fingerprint fingerprint) {
        String leagueId = structure.league().value();
        String seasonId = structure.season().value();

        jdbc.sql("insert into season_structure (league_id, season_id, fingerprint) values (?, ?, ?)")
                .param(leagueId).param(seasonId)
                .param(fingerprint.value())
                .update();

        for (Competition competition : structure.competitions()) {
            jdbc.sql("insert into competition (league_id, season_id, id, name) values (?, ?, ?, ?)")
                    .param(leagueId).param(seasonId)
                    .param(competition.id().value()).param(competition.name())
                    .update();
            for (Phase phase : competition.phases()) {
                jdbc.sql("""
                        insert into competition_phase
                        (league_id, season_id, competition_id, id, name, ordinal)
                        values (?, ?, ?, ?, ?, ?)
                        """)
                        .param(leagueId).param(seasonId).param(competition.id().value())
                        .param(phase.id().value()).param(phase.name()).param(phase.ordinal())
                        .update();
                for (CupGroup group : phase.groups()) {
                    jdbc.sql("""
                            insert into cup_group
                            (league_id, season_id, competition_id, phase_id, id, name)
                            values (?, ?, ?, ?, ?, ?)
                            """)
                            .param(leagueId).param(seasonId).param(competition.id().value())
                            .param(phase.id().value())
                            .param(group.id().value()).param(group.name())
                            .update();
                }
            }
        }

        for (SeasonPlacement placement : structure.placements()) {
            jdbc.sql("""
                    insert into season_placement
                    (league_id, season_id, club_id, conference, division)
                    values (?, ?, ?, ?, ?)
                    """)
                    .param(leagueId).param(seasonId).param(placement.club().value())
                    .param(placement.conference().name()).param(placement.division().name())
                    .update();
        }

        for (CompetitionParticipation p : structure.participations()) {
            jdbc.sql("""
                    insert into competition_participation
                    (league_id, season_id, competition_id, club_id, cup_group_id)
                    values (?, ?, ?, ?, ?)
                    """)
                    .param(leagueId).param(seasonId)
                    .param(p.competition().value()).param(p.club().value())
                    .param(p.cupGroup() == null ? null : p.cupGroup().value())
                    .update();
        }
    }

    private List<Competition> loadCompetitions(String leagueId, String seasonId) {
        var competitionRows = jdbc.sql(
                        "select id, name from competition where league_id = ? and season_id = ? order by id")
                .param(leagueId).param(seasonId)
                .query((rs, rowNum) -> new CompetitionRow(rs.getString("id"), rs.getString("name")))
                .list();

        List<Competition> competitions = new ArrayList<>(competitionRows.size());
        for (CompetitionRow row : competitionRows) {
            var phases = loadPhases(leagueId, seasonId, row.id);
            competitions.add(new Competition(new CompetitionId(row.id), row.name, phases));
        }
        return competitions;
    }

    private List<Phase> loadPhases(String leagueId, String seasonId, String competitionId) {
        var phaseRows = jdbc.sql("""
                        select id, name, ordinal from competition_phase
                        where league_id = ? and season_id = ? and competition_id = ?
                        order by ordinal
                        """)
                .param(leagueId).param(seasonId).param(competitionId)
                .query((rs, rowNum) -> new PhaseRow(
                        rs.getString("id"), rs.getString("name"), rs.getInt("ordinal")))
                .list();

        Map<String, List<CupGroup>> groupsByPhase = loadCupGroupsByPhase(leagueId, seasonId, competitionId);
        List<Phase> phases = new ArrayList<>(phaseRows.size());
        for (PhaseRow row : phaseRows) {
            phases.add(new Phase(
                    new PhaseId(row.id), row.name, row.ordinal,
                    groupsByPhase.getOrDefault(row.id, List.of())));
        }
        return phases;
    }

    private Map<String, List<CupGroup>> loadCupGroupsByPhase(String leagueId, String seasonId, String competitionId) {
        var rows = jdbc.sql("""
                        select phase_id, id, name from cup_group
                        where league_id = ? and season_id = ? and competition_id = ?
                        order by id
                        """)
                .param(leagueId).param(seasonId).param(competitionId)
                .query((rs, rowNum) -> new CupGroupRow(
                        rs.getString("phase_id"), rs.getString("id"), rs.getString("name")))
                .list();

        Map<String, List<CupGroup>> result = new HashMap<>();
        for (CupGroupRow row : rows) {
            result.computeIfAbsent(row.phaseId, k -> new ArrayList<>())
                    .add(new CupGroup(new CupGroupId(row.id), row.name));
        }
        return result;
    }

    private List<SeasonPlacement> loadPlacements(String leagueId, String seasonId) {
        return jdbc.sql("""
                        select club_id, conference, division from season_placement
                        where league_id = ? and season_id = ?
                        order by club_id
                        """)
                .param(leagueId).param(seasonId)
                .query((rs, rowNum) -> new SeasonPlacement(
                        new ClubId(rs.getString("club_id")),
                        new Conference(rs.getString("conference")),
                        new Division(rs.getString("division"))))
                .list();
    }

    private List<CompetitionParticipation> loadParticipations(String leagueId, String seasonId) {
        return jdbc.sql("""
                        select competition_id, club_id, cup_group_id from competition_participation
                        where league_id = ? and season_id = ?
                        order by competition_id, club_id
                        """)
                .param(leagueId).param(seasonId)
                .query((rs, rowNum) -> {
                    String cupGroup = rs.getString("cup_group_id");
                    return new CompetitionParticipation(
                            new ClubId(rs.getString("club_id")),
                            new CompetitionId(rs.getString("competition_id")),
                            cupGroup == null ? null : new CupGroupId(cupGroup));
                })
                .list();
    }

    private record CompetitionRow(String id, String name) {}
    private record PhaseRow(String id, String name, int ordinal) {}
    private record CupGroupRow(String phaseId, String id, String name) {}
}
