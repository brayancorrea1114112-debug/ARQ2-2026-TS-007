package com.streamhub.catalog_service.infrastructure;

import com.streamhub.catalog_service.application.ports.in.GetHomeUseCase;
import com.streamhub.catalog_service.domain.exception.ProfileNotSelectedException;
import com.streamhub.catalog_service.domain.exception.ServiceUnavailableException;
import com.streamhub.catalog_service.domain.model.ContinueWatchingItem;
import com.streamhub.catalog_service.domain.model.HomeCategory;
import com.streamhub.catalog_service.domain.model.HomeScreen;
import com.streamhub.catalog_service.domain.model.HomeSource;
import com.streamhub.catalog_service.domain.model.TitleSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/v1/home through the real filters, controller and error handler, with a fake use case. */
@SpringBootTest(properties = {"jwt.secret=" + TestTokens.SECRET, "jwt.issuer=" + TestTokens.ISSUER})
@AutoConfigureMockMvc
@ActiveProfiles("memory")
class HomeControllerTest {

    private static final String PROFILE = "prof_9921";
    private static final String CORRELATION_ID = "5b9e1c1e-6f0a-4d6b-9f55-6c0f1f3a7d11";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GetHomeUseCase getHome;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void aProfileTokenGetsTheHomeInTheFormatOfTheSpec() throws Exception {
        TitleSummary one = new TitleSummary("mov_1", "Title One", "https://cdn.example.com/p/mov_1.jpg");
        when(getHome.getHome(PROFILE)).thenReturn(new HomeScreen(PROFILE, Instant.parse("2026-10-06T19:58:00Z"),
                HomeSource.FALLBACK, List.of(new HomeCategory("Trending", List.of(one))),
                List.of(new ContinueWatchingItem("mov_8832", "ep_102", 1420), new ContinueWatchingItem("mov_5", null, 30)),
                List.of(one)));

        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.profileToken(PROFILE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile_id").value(PROFILE))
                .andExpect(jsonPath("$.generated_at").value("2026-10-06T19:58:00Z"))
                .andExpect(jsonPath("$.source").value("fallback"))
                .andExpect(jsonPath("$.categories[0].name").value("Trending"))
                .andExpect(jsonPath("$.categories[0].titles[0].content_id").value("mov_1"))
                .andExpect(jsonPath("$.categories[0].titles[0].poster").value("https://cdn.example.com/p/mov_1.jpg"))
                .andExpect(jsonPath("$.continue_watching[0].episode_id").value("ep_102"))
                .andExpect(jsonPath("$.continue_watching[0].progress_seconds").value(1420))
                .andExpect(jsonPath("$.continue_watching[1].episode_id").doesNotExist())
                .andExpect(jsonPath("$.recommended[0].content_id").value("mov_1"));
    }

    @Test
    void theCorrelationIdOfTheRequestIsEchoedAndAGeneratedOneIsUsedWhenItIsMissingOrInvalid() throws Exception {
        when(getHome.getHome(PROFILE)).thenReturn(new HomeScreen(PROFILE, Instant.now(), HomeSource.CACHE,
                List.of(), List.of(), List.of()));
        String auth = bearer(TestTokens.profileToken(PROFILE));

        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, auth).header("X-Correlation-ID", CORRELATION_ID))
                .andExpect(header().string("X-Correlation-ID", CORRELATION_ID));

        MvcResult invalid = mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, auth)
                .header("X-Correlation-ID", "not-a-uuid")).andReturn();
        String generated = invalid.getResponse().getHeader("X-Correlation-ID");
        assertEquals(36, generated.length());
    }

    @Test
    void anAccountTokenWithoutProfileIsRefusedWithProfileNotSelected() throws Exception {
        when(getHome.getHome(isNull())).thenThrow(new ProfileNotSelectedException());

        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.accountToken()))
                        .header("X-Correlation-ID", CORRELATION_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PROFILE_NOT_SELECTED"))
                .andExpect(jsonPath("$.error.correlation_id").value(CORRELATION_ID));
    }

    @Test
    void aRequestWithoutTokenIsRefusedWith401AndNeverReachesTheUseCase() throws Exception {
        mvc.perform(get("/api/v1/home").header("X-Correlation-ID", CORRELATION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.correlation_id").value(CORRELATION_ID));

        verifyNoInteractions(getHome);
    }

    @Test
    void tokensWithAWrongSignatureIssuerOrExpirationAreRefusedWith401() throws Exception {
        String otherSecret = TestTokens.sign("another-secret-with-more-than-32-bytes", TestTokens.ISSUER, PROFILE,
                Instant.now().plusSeconds(600));
        String otherIssuer = TestTokens.sign(TestTokens.SECRET, "someone-else", PROFILE, Instant.now().plusSeconds(600));
        String expired = TestTokens.sign(TestTokens.SECRET, TestTokens.ISSUER, PROFILE, Instant.now().minusSeconds(60));

        for (String token : List.of(otherSecret, otherIssuer, expired, "garbage")) {
            mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(getHome);
    }

    @Test
    void anUnavailableDependencyIsAnswered503WithTheStandardBody() throws Exception {
        when(getHome.getHome(PROFILE)).thenThrow(new ServiceUnavailableException("The catalog"));

        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.profileToken(PROFILE)))
                        .header("X-Correlation-ID", CORRELATION_ID))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.correlation_id").value(CORRELATION_ID));
    }

    @Test
    void anUnexpectedErrorIsAnswered500WithoutLeakingItsMessage() throws Exception {
        when(getHome.getHome(PROFILE)).thenThrow(new IllegalStateException("secret internals"));

        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.profileToken(PROFILE))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("An unexpected error occurred."));
    }
}
