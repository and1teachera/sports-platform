package com.sportsplatform.league.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The prepared structure of one league season. Immutable; keyed by ({@link LeagueId},
 * {@link SeasonId}). Carries the competitions with their phases, the Cup groups under
 * group-play phases, the season placements and the competition participations.
 *
 * <p>The constructor validates every invariant and reports all violations together via
 * {@link InvalidSeasonStructure}. Inputs may arrive in any order; what is stored is in canonical
 * order (competitions by id, phases by ordinal, groups by id, placements by club id,
 * participations by competition id then club id) so equality is order-insensitive and consumers
 * get a deterministic read.</p>
 */
public final class SeasonStructure {

    private final SeasonStructureId id;
    private final List<Competition> competitions;
    private final List<SeasonPlacement> placements;
    private final List<CompetitionParticipation> participations;

    public SeasonStructure(
            SeasonStructureId id,
            List<Competition> competitions,
            List<SeasonPlacement> placements,
            List<CompetitionParticipation> participations
    ) {
        this.id = Objects.requireNonNull(id, "id");
        Objects.requireNonNull(competitions, "competitions");
        Objects.requireNonNull(placements, "placements");
        Objects.requireNonNull(participations, "participations");

        List<Competition> canonicalCompetitions = canonicaliseCompetitions(competitions);
        List<SeasonPlacement> canonicalPlacements = sorted(
                placements, Comparator.comparing(p -> p.club().value()));
        List<CompetitionParticipation> canonicalParticipations = sorted(
                participations,
                Comparator
                        .<CompetitionParticipation, String>comparing(p -> p.competition().value())
                        .thenComparing(p -> p.club().value())
        );

        List<String> violations = new ArrayList<>();
        validateCompetitionsAndPhases(canonicalCompetitions, violations);
        validateCupGroups(canonicalCompetitions, violations);
        validatePlacements(canonicalPlacements, violations);
        validateParticipations(canonicalCompetitions, canonicalPlacements, canonicalParticipations, violations);

        if (!violations.isEmpty()) {
            throw new InvalidSeasonStructure(violations);
        }

        this.competitions = List.copyOf(canonicalCompetitions);
        this.placements = List.copyOf(canonicalPlacements);
        this.participations = List.copyOf(canonicalParticipations);
    }

    public SeasonStructureId id() { return id; }
    public LeagueId league() { return id.league(); }
    public SeasonId season() { return id.season(); }
    public List<Competition> competitions() { return competitions; }
    public List<SeasonPlacement> placements() { return placements; }
    public List<CompetitionParticipation> participations() { return participations; }

    private static List<Competition> canonicaliseCompetitions(List<Competition> input) {
        List<Competition> result = new ArrayList<>(input.size());
        for (Competition competition : input) {
            Objects.requireNonNull(competition, "competition");
            List<Phase> sortedPhases = new ArrayList<>(competition.phases().size());
            for (Phase phase : competition.phases()) {
                Objects.requireNonNull(phase, "phase");
                List<CupGroup> sortedGroups = sorted(
                        phase.groups(), Comparator.comparing(g -> g.id().value()));
                sortedPhases.add(new Phase(phase.id(), phase.name(), phase.ordinal(), sortedGroups));
            }
            sortedPhases.sort(Comparator.comparingInt(Phase::ordinal)
                    .thenComparing(p -> p.id().value()));
            result.add(new Competition(competition.id(), competition.name(), sortedPhases));
        }
        result.sort(Comparator.comparing(c -> c.id().value()));
        return result;
    }

    private static <T> List<T> sorted(List<T> input, Comparator<? super T> order) {
        List<T> copy = new ArrayList<>(input);
        copy.sort(order);
        return copy;
    }

    // Invariants 1 and 2.
    private static void validateCompetitionsAndPhases(List<Competition> competitions, List<String> violations) {
        Set<String> competitionIds = new HashSet<>();
        for (Competition competition : competitions) {
            if (!competitionIds.add(competition.id().value())) {
                violations.add("duplicate competition id: " + competition.id().value());
            }
            if (competition.phases().isEmpty()) {
                violations.add("competition " + competition.id().value() + " has no phases");
                continue;
            }
            Set<String> phaseIds = new HashSet<>();
            Set<Integer> phaseOrdinals = new HashSet<>();
            for (Phase phase : competition.phases()) {
                if (!phaseIds.add(phase.id().value())) {
                    violations.add("duplicate phase id " + phase.id().value()
                            + " in competition " + competition.id().value());
                }
                if (!phaseOrdinals.add(phase.ordinal())) {
                    violations.add("duplicate phase ordinal " + phase.ordinal()
                            + " in competition " + competition.id().value());
                }
            }
        }
    }

    // Invariant 3.
    private static void validateCupGroups(List<Competition> competitions, List<String> violations) {
        for (Competition competition : competitions) {
            Set<String> groupIds = new HashSet<>();
            Set<String> groupNames = new HashSet<>();
            for (Phase phase : competition.phases()) {
                for (CupGroup group : phase.groups()) {
                    if (!groupIds.add(group.id().value())) {
                        violations.add("duplicate cup group id " + group.id().value()
                                + " in competition " + competition.id().value());
                    }
                    if (!groupNames.add(group.name())) {
                        violations.add("duplicate cup group name '" + group.name()
                                + "' in competition " + competition.id().value());
                    }
                }
            }
        }
    }

    // Invariant 4.
    private static void validatePlacements(List<SeasonPlacement> placements, List<String> violations) {
        Set<String> clubs = new HashSet<>();
        for (SeasonPlacement placement : placements) {
            if (!clubs.add(placement.club().value())) {
                violations.add("more than one placement for club " + placement.club().value());
            }
        }
    }

    // Invariants 5, 6, 7, 8.
    private static void validateParticipations(
            List<Competition> competitions,
            List<SeasonPlacement> placements,
            List<CompetitionParticipation> participations,
            List<String> violations
    ) {
        Map<String, Competition> competitionById = new HashMap<>();
        for (Competition competition : competitions) {
            competitionById.put(competition.id().value(), competition);
        }
        Set<String> placedClubs = new HashSet<>();
        for (SeasonPlacement placement : placements) {
            placedClubs.add(placement.club().value());
        }
        Set<String> seenPairs = new HashSet<>();
        for (CompetitionParticipation p : participations) {
            Competition competition = competitionById.get(p.competition().value());
            if (competition == null) {
                violations.add("participation names unknown competition " + p.competition().value()
                        + " for club " + p.club().value());
                continue;
            }
            if (!placedClubs.contains(p.club().value())) {
                violations.add("participation names club " + p.club().value()
                        + " which has no placement in this structure");
            }
            String pairKey = p.competition().value() + "|" + p.club().value();
            if (!seenPairs.add(pairKey)) {
                violations.add("more than one participation for club " + p.club().value()
                        + " in competition " + p.competition().value());
            }
            if (p.cupGroup() != null) {
                boolean cupGroupBelongs = false;
                for (Phase phase : competition.phases()) {
                    for (CupGroup group : phase.groups()) {
                        if (group.id().value().equals(p.cupGroup().value())) {
                            cupGroupBelongs = true;
                            break;
                        }
                    }
                    if (cupGroupBelongs) break;
                }
                if (!cupGroupBelongs) {
                    violations.add("participation of club " + p.club().value()
                            + " in " + p.competition().value()
                            + " names cup group " + p.cupGroup().value()
                            + " which is not a group of that competition");
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeasonStructure other)) return false;
        return id.equals(other.id)
                && competitions.equals(other.competitions)
                && placements.equals(other.placements)
                && participations.equals(other.participations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, competitions, placements, participations);
    }
}
