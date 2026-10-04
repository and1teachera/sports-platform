package com.sportsplatform.league.infrastructure.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sportsplatform.league.application.PreparedStructure;
import com.sportsplatform.league.domain.Club;
import com.sportsplatform.league.domain.ClubId;
import com.sportsplatform.league.domain.Competition;
import com.sportsplatform.league.domain.CompetitionId;
import com.sportsplatform.league.domain.CompetitionParticipation;
import com.sportsplatform.league.domain.Conference;
import com.sportsplatform.league.domain.CupGroup;
import com.sportsplatform.league.domain.CupGroupId;
import com.sportsplatform.league.domain.Division;
import com.sportsplatform.league.domain.League;
import com.sportsplatform.league.domain.LeagueId;
import com.sportsplatform.league.domain.Phase;
import com.sportsplatform.league.domain.PhaseId;
import com.sportsplatform.league.domain.SeasonId;
import com.sportsplatform.league.domain.SeasonPlacement;
import com.sportsplatform.league.domain.SeasonStructure;
import com.sportsplatform.league.domain.SeasonStructureId;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads a {@link SeedDocument} from a YAML {@link Resource} and maps it to a
 * {@link PreparedStructure} ready for the apply operation.
 *
 * <p>Mapping failures carry the offending field or section in the exception message so an
 * operator can find the problem in the seed quickly.</p>
 */
@Component
public class PreparedStructureLoader {

    private final ObjectMapper yaml;

    public PreparedStructureLoader() {
        this.yaml = new ObjectMapper(new YAMLFactory());
    }

    public PreparedStructure load(Resource resource) {
        Objects.requireNonNull(resource, "resource");
        SeedDocument document = read(resource);
        return map(document);
    }

    private SeedDocument read(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            SeedDocument document = yaml.readValue(in, SeedDocument.class);
            if (document == null) {
                throw new SeedParseException(describe(resource) + ": empty seed document");
            }
            return document;
        } catch (IOException e) {
            throw new SeedParseException(describe(resource) + ": failed to read seed", e);
        }
    }

    private PreparedStructure map(SeedDocument document) {
        requireSection(document.league(), "league");
        requireSection(document.season(), "season");

        LeagueId leagueId = new LeagueId(document.league().id());
        SeasonId seasonId = new SeasonId(document.season().id());
        SeasonStructureId structureId = new SeasonStructureId(leagueId, seasonId);

        League league = new League(leagueId, document.league().name(), null);
        List<Club> clubs = mapClubs(document.clubs(), leagueId);
        List<Competition> competitions = mapCompetitions(document.competitions());
        List<SeasonPlacement> placements = mapPlacements(document.placements());
        List<CompetitionParticipation> participations = mapParticipations(document.participations());

        SeasonStructure structure = new SeasonStructure(structureId, competitions, placements, participations);
        return new PreparedStructure(league, clubs, structure);
    }

    private static List<Club> mapClubs(List<SeedDocument.ClubSection> sections, LeagueId leagueId) {
        if (sections == null) return List.of();
        List<Club> clubs = new ArrayList<>(sections.size());
        for (SeedDocument.ClubSection s : sections) {
            try {
                clubs.add(new Club(new ClubId(s.id()), leagueId, s.name()));
            } catch (RuntimeException e) {
                throw new SeedParseException("club " + s.id() + ": " + e.getMessage(), e);
            }
        }
        return clubs;
    }

    private static List<Competition> mapCompetitions(List<SeedDocument.CompetitionSection> sections) {
        if (sections == null) return List.of();
        List<Competition> competitions = new ArrayList<>(sections.size());
        for (SeedDocument.CompetitionSection c : sections) {
            try {
                List<Phase> phases = mapPhases(c.phases(), c.id());
                competitions.add(new Competition(new CompetitionId(c.id()), c.name(), phases));
            } catch (RuntimeException e) {
                throw new SeedParseException("competition " + c.id() + ": " + e.getMessage(), e);
            }
        }
        return competitions;
    }

    private static List<Phase> mapPhases(List<SeedDocument.PhaseSection> sections, String competitionId) {
        if (sections == null) return List.of();
        List<Phase> phases = new ArrayList<>(sections.size());
        for (SeedDocument.PhaseSection p : sections) {
            try {
                List<CupGroup> groups = mapGroups(p.groups());
                phases.add(new Phase(new PhaseId(p.id()), p.name(), p.ordinal(), groups));
            } catch (RuntimeException e) {
                throw new SeedParseException(
                        "competition " + competitionId + " phase " + p.id() + ": " + e.getMessage(), e);
            }
        }
        return phases;
    }

    private static List<CupGroup> mapGroups(List<SeedDocument.GroupSection> sections) {
        if (sections == null) return List.of();
        List<CupGroup> groups = new ArrayList<>(sections.size());
        for (SeedDocument.GroupSection g : sections) {
            groups.add(new CupGroup(new CupGroupId(g.id()), g.name()));
        }
        return groups;
    }

    private static List<SeasonPlacement> mapPlacements(List<SeedDocument.PlacementSection> sections) {
        if (sections == null) return List.of();
        List<SeasonPlacement> placements = new ArrayList<>(sections.size());
        for (SeedDocument.PlacementSection p : sections) {
            try {
                placements.add(new SeasonPlacement(
                        new ClubId(p.club()),
                        new Conference(p.conference()),
                        new Division(p.division())));
            } catch (RuntimeException e) {
                throw new SeedParseException("placement for club " + p.club() + ": " + e.getMessage(), e);
            }
        }
        return placements;
    }

    private static List<CompetitionParticipation> mapParticipations(List<SeedDocument.ParticipationSection> sections) {
        if (sections == null) return List.of();
        List<CompetitionParticipation> participations = new ArrayList<>(sections.size());
        for (SeedDocument.ParticipationSection p : sections) {
            try {
                ClubId club = new ClubId(p.club());
                CompetitionId competition = new CompetitionId(p.competition());
                if (p.cupGroup() == null || p.cupGroup().isBlank()) {
                    participations.add(CompetitionParticipation.inCompetition(club, competition));
                } else {
                    participations.add(CompetitionParticipation.inCupGroup(
                            club, competition, new CupGroupId(p.cupGroup())));
                }
            } catch (RuntimeException e) {
                throw new SeedParseException(
                        "participation for club " + p.club()
                                + " in " + p.competition() + ": " + e.getMessage(), e);
            }
        }
        return participations;
    }

    private static void requireSection(Object section, String name) {
        if (section == null) {
            throw new SeedParseException("seed is missing the " + name + " section");
        }
    }

    private static String describe(Resource resource) {
        try {
            return resource.getURI().toString();
        } catch (IOException e) {
            return resource.getDescription();
        }
    }
}
