package com.streamhub.telemetry_service.infrastructure;

import com.streamhub.telemetry_service.infrastructure.fixtures.InMemoryEventPublisherAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/v1/telemetry/ping through the real filters, controller and error handler. */
@SpringBootTest(properties = {
        "jwt.secret=" + TestTokens.SECRET,
        "jwt.issuer=" + TestTokens.ISSUER,
        "spring.rabbitmq.dynamic=false",
        "management.tracing.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("memory")
class TelemetryControllerTest {

    private static final String PROFILE = "prof_9921";
    private static final String EVENT_ID = "0f6c2a52-6a53-4f7e-8a56-9d8c6a6b5f10";
    private static final String CORRELATION_ID = "5b9e1c1e-6f0a-4d6b-9f55-6c0f1f3a7d11";
    private static final String TRACEPARENT =
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private InMemoryEventPublisherAdapter publisher;

    @BeforeEach
    void resetPublisher() {
        publisher.clear();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String pingBody(String eventId, int progressSeconds) {
        return """
                {
                  "event_id": "%s",
                  "content_id": "mov_8832",
                  "episode_id": "ep_102",
                  "progress_seconds": %d,
                  "timestamp": "2026-10-06T20:00:00Z"
                }
                """.formatted(eventId, progressSeconds);
    }

    @Test
    void aProfileTokenGets202AndThePingIsPublished() throws Exception {
        mvc.perform(post("/api/v1/telemetry/ping")
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.profileToken(PROFILE)))
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .header("traceparent", TRACEPARENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pingBody(EVENT_ID, 1420)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Correlation-ID", CORRELATION_ID))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.event_id").value(EVENT_ID))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        assertThat(publisher.pings()).hasSize(1);
        InMemoryEventPublisherAdapter.PublishedPing published = publisher.pings().get(0);
        assertThat(published.profileId()).isEqualTo(PROFILE);
        assertThat(published.command().eventId()).isEqualTo(EVENT_ID);
        assertThat(published.command().contentId()).isEqualTo("mov_8832");
        assertThat(published.command().episodeId()).isEqualTo("ep_102");
        assertThat(published.command().progressSeconds()).isEqualTo(1420);
        assertThat(published.command().timestamp()).isEqualTo(Instant.parse("2026-10-06T20:00:00Z"));
        assertThat(published.context().correlationId()).isEqualTo(CORRELATION_ID);
        assertThat(published.context().traceparent()).isEqualTo(TRACEPARENT);
    }

    @Test
    void anAccountTokenWithoutProfileIsRefusedWith403() throws Exception {
        mvc.perform(post("/api/v1/telemetry/ping")
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.accountToken()))
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pingBody(EVENT_ID, 1420)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PROFILE_NOT_SELECTED"))
                .andExpect(jsonPath("$.error.correlation_id").value(CORRELATION_ID));

        assertThat(publisher.pings()).isEmpty();
    }

    @Test
    void aRequestWithoutTokenIsRefusedWith401() throws Exception {
        mvc.perform(post("/api/v1/telemetry/ping")
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pingBody(EVENT_ID, 1420)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.correlation_id").value(CORRELATION_ID));

        assertThat(publisher.pings()).isEmpty();
    }

    @Test
    void anEventIdThatIsNotUuidV4IsRefusedWith400() throws Exception {
        mvc.perform(post("/api/v1/telemetry/ping")
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.profileToken(PROFILE)))
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pingBody("not-a-uuid", 1420)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.correlation_id").value(CORRELATION_ID));

        assertThat(publisher.pings()).isEmpty();
    }

    @Test
    void negativeProgressIsRefusedWith400() throws Exception {
        mvc.perform(post("/api/v1/telemetry/ping")
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.profileToken(PROFILE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pingBody(EVENT_ID, -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        assertThat(publisher.pings()).isEmpty();
    }

    @Test
    void aBrokerThatDoesNotConfirmIsAnsweredAs503() throws Exception {
        publisher.failNextPublication();

        mvc.perform(post("/api/v1/telemetry/ping")
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.profileToken(PROFILE)))
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pingBody(EVENT_ID, 1420)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.correlation_id").value(CORRELATION_ID));
    }

    @Test
    void aGeneratedCorrelationIdIsUsedInThePublishedEventWhenTheHeaderIsMissing() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/telemetry/ping")
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.profileToken(PROFILE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pingBody(EVENT_ID, 1420)))
                .andExpect(status().isAccepted())
                .andReturn();

        String generated = result.getResponse().getHeader("X-Correlation-ID");
        assertThat(generated).isNotBlank();
        assertThat(publisher.pings().get(0).context().correlationId()).isEqualTo(generated);
    }

    @Test
    void tokensWithAWrongSignatureIssuerOrExpirationAreRefusedWith401() throws Exception {
        String otherSecret = TestTokens.sign("another-secret-with-more-than-32-bytes-0123456789",
                TestTokens.ISSUER, PROFILE, Instant.now().plusSeconds(600));
        String otherIssuer = TestTokens.sign(TestTokens.SECRET, "someone-else", PROFILE,
                Instant.now().plusSeconds(600));
        String expired = TestTokens.sign(TestTokens.SECRET, TestTokens.ISSUER, PROFILE,
                Instant.now().minusSeconds(60));

        for (String token : new String[]{otherSecret, otherIssuer, expired, "garbage"}) {
            mvc.perform(post("/api/v1/telemetry/ping")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(pingBody(EVENT_ID, 1420)))
                    .andExpect(status().isUnauthorized());
        }

        assertThat(publisher.pings()).isEmpty();
    }
}
