package com.sportsplatform;

import com.sportsplatform.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API security contract: deny-by-default. Only the paths listed explicitly in the security
 * configuration are reachable without authentication; every other path (including unmapped ones
 * and other actuator endpoints) is denied.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @DisplayName("The health endpoint is reachable without authentication")
    @Test
    void shouldExposeHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @DisplayName("An unmapped endpoint is denied")
    @Test
    void shouldDenyUnmappedEndpoint() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().is(either401Or403()));
    }

    @DisplayName("The metrics endpoint is denied")
    @Test
    void shouldDenyMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().is(either401Or403()));
    }

    @DisplayName("The info endpoint is denied")
    @Test
    void shouldDenyInfoEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().is(either401Or403()));
    }

    private static org.hamcrest.Matcher<Integer> either401Or403() {
        return org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.is(401), org.hamcrest.Matchers.is(403));
    }
}
