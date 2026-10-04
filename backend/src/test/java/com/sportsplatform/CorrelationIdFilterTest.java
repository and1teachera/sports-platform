package com.sportsplatform;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sportsplatform.observability.CorrelationIdFilter;
import com.sportsplatform.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Correlation-ID filter contract: the filter honours an incoming X-Correlation-Id, generates a
 * UUID when the header is absent, echoes it on the response, populates the SLF4J MDC during
 * request handling, and clears the MDC after the request completes.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorrelationIdFilterTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private Logger filterLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        filterLogger = (Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
        appender = new ListAppender<>();
        appender.start();
        filterLogger.addAppender(appender);
        filterLogger.setLevel(Level.INFO);
    }

    @AfterEach
    void detachAppender() {
        filterLogger.detachAppender(appender);
        MDC.remove(CorrelationIdFilter.MDC_KEY);
    }

    @Test
    void an_incoming_correlation_id_is_echoed_on_the_response() throws Exception {
        mockMvc.perform(get("/actuator/health").header(CorrelationIdFilter.HEADER, "abc-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER, "abc-123"));
    }

    @Test
    void an_absent_correlation_id_is_generated_as_a_uuid() throws Exception {
        var result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn();

        String generated = result.getResponse().getHeader(CorrelationIdFilter.HEADER);
        assertThat(generated)
                .as("response must carry a generated correlation id")
                .isNotNull()
                .matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    @Test
    void mdc_is_populated_during_request_handling() throws Exception {
        mockMvc.perform(get("/actuator/health").header(CorrelationIdFilter.HEADER, "trace-xyz"))
                .andExpect(status().isOk());

        List<ILoggingEvent> events = appender.list;
        assertThat(events)
                .as("filter must emit at least one log event while handling the request")
                .isNotEmpty();
        assertThat(events)
                .anyMatch(e -> "trace-xyz".equals(e.getMDCPropertyMap().get(CorrelationIdFilter.MDC_KEY)));
    }

    @Test
    void mdc_is_cleared_after_the_request_completes() throws Exception {
        mockMvc.perform(get("/actuator/health").header(CorrelationIdFilter.HEADER, "cleanup-check"))
                .andExpect(status().isOk());

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY))
                .as("filter must clear the MDC entry in its finally block")
                .isNull();
    }
}
