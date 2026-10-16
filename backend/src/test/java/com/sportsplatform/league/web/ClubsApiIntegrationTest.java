package com.sportsplatform.league.web;

import com.sportsplatform.league.application.ApplyPreparedStructure;
import com.sportsplatform.league.domain.CorrelationReApplier;
import com.sportsplatform.league.infrastructure.seed.PreparedStructureLoader;
import com.sportsplatform.support.DatabaseCleanedIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The clubs endpoint against PostgreSQL, seeded through the apply operation with the YAML fixture.
 * In that fixture every club takes part in both the Regular Season and the NBA Cup, so each club
 * has two competition participations and one season placement.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClubsApiIntegrationTest extends DatabaseCleanedIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PreparedStructureLoader loader;
    @Autowired private ApplyPreparedStructure apply;
    @Autowired private JdbcClient jdbc;
    @MockitoSpyBean private CorrelationReApplier correlations;

    @DisplayName("The clubs of the default league's current season are served with their season context")
    @Test
    void shouldServeCurrentSeasonClubsOfDefaultLeague() throws Exception {
        applyFixture();

        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.league").value("nba"))
                .andExpect(jsonPath("$.season").value("2026-2027"))
                .andExpect(jsonPath("$.clubs[*].name")
                        .value(contains("Boston Celtics", "Chicago Bulls", "Golden State Warriors")));
    }

    @DisplayName("Each club appears exactly once although it takes part in several competitions")
    @Test
    void shouldReturnOneRowPerClubDespiteSeveralParticipations() throws Exception {
        applyFixture();
        assertThat(participationsOf("club-001")).isEqualTo(2);
        assertThat(participationsOf("club-006")).isEqualTo(2);
        assertThat(participationsOf("club-021")).isEqualTo(2);

        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs", hasSize(3)))
                .andExpect(jsonPath("$.clubs[*].id").value(contains("club-001", "club-006", "club-021")));
    }

    @DisplayName("Conference and division come from each club's season placement")
    @Test
    void shouldTakeConferenceAndDivisionFromSeasonPlacement() throws Exception {
        applyFixture();

        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs[0].conference").value("Eastern Conference"))
                .andExpect(jsonPath("$.clubs[0].division").value("Atlantic"))
                .andExpect(jsonPath("$.clubs[1].conference").value("Eastern Conference"))
                .andExpect(jsonPath("$.clubs[1].division").value("Central"))
                .andExpect(jsonPath("$.clubs[2].conference").value("Western Conference"))
                .andExpect(jsonPath("$.clubs[2].division").value("Pacific"));
    }

    @DisplayName("A club's id is its lasting identity from the prepared data")
    @Test
    void shouldReturnDomainClubIdentity() throws Exception {
        applyFixture();
        var storedIds = jdbc.sql("select id from club order by id").query(String.class).list();
        assertThat(storedIds).containsExactly("club-001", "club-006", "club-021");

        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs[*].id").value(contains(storedIds.toArray())));
    }

    @DisplayName("The response carries only the season context and the club fields, nothing provider-related")
    @Test
    void shouldExposeNoProviderCorrelationOrParticipationFields() throws Exception {
        applyFixture();

        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$", hasKey("league")))
                .andExpect(jsonPath("$", hasKey("season")))
                .andExpect(jsonPath("$", hasKey("clubs")))
                .andExpect(jsonPath("$.clubs[*]", everyItem(aMapWithSize(4))))
                .andExpect(jsonPath("$.clubs[0].*", hasSize(4)))
                .andExpect(jsonPath("$.clubs[0]", hasKey("id")))
                .andExpect(jsonPath("$.clubs[0]", hasKey("name")))
                .andExpect(jsonPath("$.clubs[0]", hasKey("conference")))
                .andExpect(jsonPath("$.clubs[0]", hasKey("division")));
    }

    @DisplayName("Serving the clubs never invokes the provider correlation seam")
    @Test
    void shouldInvokeNoProviderPathWhenServingClubs() throws Exception {
        applyFixture();
        clearInvocations(correlations);

        mockMvc.perform(get("/api/clubs")).andExpect(status().isOk());

        verifyNoInteractions(correlations);
    }

    @DisplayName("Before any prepared structure is applied the default league has no current season to serve")
    @Test
    void shouldRespondNotFoundWithoutCurrentSeason() throws Exception {
        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isNotFound());
    }

    @DisplayName("The clubs endpoint is readable without authentication but only by GET")
    @Test
    void shouldPermitOnlyGetOnClubs() throws Exception {
        applyFixture();

        mockMvc.perform(get("/api/clubs")).andExpect(status().isOk());
        mockMvc.perform(post("/api/clubs")).andExpect(status().is(anyOf(is(401), is(403))));
    }

    private void applyFixture() {
        apply.apply(loader.load(new ClassPathResource("seed/test-fixture.yaml")));
    }

    private int participationsOf(String clubId) {
        return jdbc.sql("select count(*) from competition_participation where club_id = ?")
                .param(clubId)
                .query(Integer.class)
                .single();
    }
}
