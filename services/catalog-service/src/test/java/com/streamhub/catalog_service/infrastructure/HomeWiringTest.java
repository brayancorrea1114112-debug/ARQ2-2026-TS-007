package com.streamhub.catalog_service.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The real use case wired to the sample adapters of the "memory" profile, called with real tokens. */
@SpringBootTest(properties = {"jwt.secret=" + TestTokens.SECRET, "jwt.issuer=" + TestTokens.ISSUER})
@AutoConfigureMockMvc
@ActiveProfiles("memory")
class HomeWiringTest {

    @Autowired
    private MockMvc mvc;

    private static String bearer(String profileId) {
        return "Bearer " + TestTokens.profileToken(profileId);
    }

    @Test
    void aProfileWithAViewGetsItCompletedWithTrending() throws Exception {
        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer("prof_9921")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile_id").value("prof_9921"))
                .andExpect(jsonPath("$.source").value("cache"))
                .andExpect(jsonPath("$.categories[0].name").value("Trending"))
                .andExpect(jsonPath("$.categories[0].titles[0].content_id").value("mov_1"))
                .andExpect(jsonPath("$.continue_watching[0].episode_id").value("ep_102"))
                .andExpect(jsonPath("$.recommended[0].name").value("Paper Cities"));
    }

    @Test
    void aProfileWithoutViewGetsTheFallbackHome() throws Exception {
        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer("prof_1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile_id").value("prof_1234"))
                .andExpect(jsonPath("$.source").value("fallback"))
                .andExpect(jsonPath("$.categories[0].name").value("Trending"))
                .andExpect(jsonPath("$.continue_watching").isEmpty());
    }
}
