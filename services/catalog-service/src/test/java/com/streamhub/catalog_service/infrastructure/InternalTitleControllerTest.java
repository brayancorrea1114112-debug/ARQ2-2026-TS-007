package com.streamhub.catalog_service.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /internal/v1/titles/genres through the real filters and controller, with the in-memory catalog. */
@SpringBootTest(properties = {"jwt.secret=" + TestTokens.SECRET, "jwt.issuer=" + TestTokens.ISSUER})
@AutoConfigureMockMvc
@ActiveProfiles("memory")
class InternalTitleControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void theGenresOfEveryTitleAreListedWithoutAToken() throws Exception {
        mvc.perform(get("/internal/v1/titles/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titles.length()").value(3))
                .andExpect(jsonPath("$.titles[0].content_id").value("mov_1"))
                .andExpect(jsonPath("$.titles[0].genres[0]").value("Drama"))
                .andExpect(jsonPath("$.titles[0].genres[1]").value("Thriller"))
                .andExpect(jsonPath("$.titles[2].genres.length()").value(0));
    }

    @Test
    void theRestOfTheApiStillNeedsAToken() throws Exception {
        mvc.perform(get("/internal/v1/titles/other"))
                .andExpect(status().isUnauthorized());
    }
}
