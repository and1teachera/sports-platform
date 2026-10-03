package com.sportsplatform;

import com.sportsplatform.support.AbstractIntegrationTest;
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

    @Test
    void health_is_reachable_without_authentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void an_unmapped_endpoint_is_denied() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().is(either401Or403()));
    }

    @Test
    void metrics_endpoint_is_denied() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().is(either401Or403()));
    }

    @Test
    void info_endpoint_is_denied() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().is(either401Or403()));
    }

    private static org.hamcrest.Matcher<Integer> either401Or403() {
        return org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.is(401), org.hamcrest.Matchers.is(403));
    }
}
