package com.streamhub.telemetry_service.infrastructure;

import com.streamhub.telemetry_service.domain.model.Rating;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/v1/interactions/like through the real filters, controller and error handler. */
@SpringBootTest(properties = {
        "jwt.secret=" + TestTokens.SECRET,
        "jwt.issuer=" + TestTokens.ISSUER,
        "spring.rabbitmq.dynamic=false",
        "management.tracing.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("memory")
class InteractionControllerTest {

    private static final String PROFILE = "prof_9921";
    private static final String EVENT_ID = "a3b1b8d4-2c9f-4a53-b2f5-0a5c8e4d7a90";
    private static final String CORRELATION_ID = "5b9e1c1e-6f0a-4d6b-9f55-6c0f1f3a7d11";

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

    private static String ratingBody(String rating) {
        return """
                {
                  "event_id": "%s",
                  "content_id": "mov_8832",
                  "rating": "%s",
                  "timestamp": "2026-10-06T20:01:15Z"
                }
                """.formatted(EVENT_ID, rating);
    }

    @Test
    void aProfileTokenGets202AndTheRatingIsPublished() throws Exception {
        mvc.perform(post("/api/v1/interactions/like")
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.profileToken(PROFILE)))
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ratingBody("LIKE")))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Correlation-ID", CORRELATION_ID))
                .andExpect(jsonPath("$.event_id").value(EVENT_ID))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        assertThat(publisher.ratings()).hasSize(1);
        InMemoryEventPublisherAdapter.PublishedRating published = publisher.ratings().get(0);
        assertThat(published.profileId()).isEqualTo(PROFILE);
        assertThat(published.command().eventId()).isEqualTo(EVENT_ID);
        assertThat(published.command().contentId()).isEqualTo("mov_8832");
        assertThat(published.command().rating()).isEqualTo(Rating.LIKE);
        assertThat(published.command().timestamp()).isEqualTo(Instant.parse("2026-10-06T20:01:15Z"));
        assertThat(published.context().correlationId()).isEqualTo(CORRELATION_ID);
    }

    @Test
    void anInvalidRatingIsRefusedWith400() throws Exception {
        mvc.perform(post("/api/v1/interactions/like")
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.profileToken(PROFILE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ratingBody("LOVE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        assertThat(publisher.ratings()).isEmpty();
    }

    @Test
    void anAccountTokenWithoutProfileIsRefusedWith403() throws Exception {
        mvc.perform(post("/api/v1/interactions/like")
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.accountToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ratingBody("DISLIKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PROFILE_NOT_SELECTED"));

        assertThat(publisher.ratings()).isEmpty();
    }
}
