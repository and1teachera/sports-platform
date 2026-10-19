package com.sportsplatform.league.infrastructure.seed;

import com.sportsplatform.league.application.ApplyOutcome;
import com.sportsplatform.league.application.ApplyPreparedStructure;
import com.sportsplatform.league.application.PreparedStructure;
import com.sportsplatform.league.domain.Club;
import com.sportsplatform.league.domain.CompetitionParticipation;
import com.sportsplatform.league.domain.CupGroupId;
import com.sportsplatform.league.domain.SeasonPlacement;
import com.sportsplatform.support.DatabaseCleanedIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The committed 2026-27 NBA prepared structure: it loads through the seed loader, holds the
 * whole league with one placement per club and every club in the Regular Season and in one NBA
 * Cup group, and once applied is what the clubs endpoint serves.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PreparedSeasonSeedIntegrationTest extends DatabaseCleanedIntegrationTest {

    private static final String SEED = "seed/prepared-2026-2027.yaml";
    private static final String REGULAR_SEASON = "regular-season-2026-2027";
    private static final String NBA_CUP = "nba-cup-2026";
    private static final List<String> CUP_GROUPS = List.of(
            "east-group-a", "east-group-b", "east-group-c",
            "west-group-a", "west-group-b", "west-group-c");

    @Autowired private PreparedStructureLoader loader;
    @Autowired private ApplyPreparedStructure apply;
    @Autowired private MockMvc mockMvc;

    @DisplayName("The committed 2026-27 seed loads through the seed loader")
    @Test
    void shouldLoadCommittedSeed() {
        var prepared = load();

        assertThat(prepared.league().id().value()).isEqualTo("nba");
        assertThat(prepared.structure().season().value()).isEqualTo("2026-2027");
    }

    @DisplayName("The seed holds thirty clubs with distinct ids club-001 to club-030")
    @Test
    void shouldHoldThirtyDistinctClubs() {
        var ids = load().clubs().stream().map(Club::id).map(id -> id.value()).toList();

        assertThat(ids).containsExactlyInAnyOrderElementsOf(
                IntStream.rangeClosed(1, 30).mapToObj(i -> "club-%03d".formatted(i)).toList());
    }

    @DisplayName("The seed holds one season placement per club")
    @Test
    void shouldHoldOnePlacementPerClub() {
        var prepared = load();
        var placedClubs = prepared.structure().placements().stream()
                .map(SeasonPlacement::club).collect(Collectors.toSet());

        assertThat(prepared.structure().placements()).hasSize(30);
        assertThat(placedClubs).containsExactlyInAnyOrderElementsOf(
                prepared.clubs().stream().map(Club::id).toList());
    }

    @DisplayName("Every club takes part in the Regular Season and the NBA Cup, and in nothing else yet")
    @Test
    void shouldHoldSixtyParticipations() {
        Map<String, Long> byCompetition = load().structure().participations().stream()
                .collect(Collectors.groupingBy(p -> p.competition().value(), Collectors.counting()));

        assertThat(byCompetition).containsOnly(Map.entry(REGULAR_SEASON, 30L), Map.entry(NBA_CUP, 30L));
    }

    @DisplayName("Every NBA Cup participation names one of the six announced groups, five clubs each")
    @Test
    void shouldAssignEveryCupParticipationToAnnouncedGroup() {
        List<CompetitionParticipation> cup = load().structure().participations().stream()
                .filter(p -> p.competition().value().equals(NBA_CUP))
                .toList();
        Map<String, Long> clubsPerGroup = cup.stream()
                .map(p -> p.cupGroupId().map(CupGroupId::value).orElse("none"))
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));

        assertThat(clubsPerGroup).containsOnlyKeys(CUP_GROUPS);
        assertThat(clubsPerGroup.values()).containsOnly(5L);
    }

    @DisplayName("Once applied, the clubs endpoint serves all thirty clubs of the season")
    @Test
    void shouldServeAllThirtyClubsAfterApply() throws Exception {
        assertThat(apply.apply(load())).isInstanceOf(ApplyOutcome.Applied.class);

        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.league").value("nba"))
                .andExpect(jsonPath("$.season").value("2026-2027"))
                .andExpect(jsonPath("$.clubs", hasSize(30)));
    }

    @DisplayName("The Lakers, the Celtics and the Bucks are served with their 2026-27 conference and division")
    @Test
    void shouldServeSpotCheckedPlacements() throws Exception {
        apply.apply(load());

        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs[?(@.id == 'club-023')].name").value("Los Angeles Lakers"))
                .andExpect(jsonPath("$.clubs[?(@.id == 'club-023')].conference").value("Western Conference"))
                .andExpect(jsonPath("$.clubs[?(@.id == 'club-023')].division").value("Pacific"))
                .andExpect(jsonPath("$.clubs[?(@.id == 'club-001')].name").value("Boston Celtics"))
                .andExpect(jsonPath("$.clubs[?(@.id == 'club-001')].conference").value("Eastern Conference"))
                .andExpect(jsonPath("$.clubs[?(@.id == 'club-001')].division").value("Atlantic"))
                .andExpect(jsonPath("$.clubs[?(@.id == 'club-010')].name").value("Milwaukee Bucks"))
                .andExpect(jsonPath("$.clubs[?(@.id == 'club-010')].conference").value("Eastern Conference"))
                .andExpect(jsonPath("$.clubs[?(@.id == 'club-010')].division").value("Central"));
    }

    @DisplayName("Applying the committed seed a second time leaves the store unchanged")
    @Test
    void shouldLeaveSecondApplyUnchanged() {
        apply.apply(load());

        assertThat(apply.apply(load())).isInstanceOf(ApplyOutcome.Unchanged.class);
    }

    private PreparedStructure load() {
        return loader.load(new ClassPathResource(SEED));
    }
}
