package com.streamhub.catalog_service.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks of the shapes that spec.md fixes (1.3, 1.4 and 3.4), through the real filters and the sample adapters.
 * The scenarios of Feature 2 are covered by HomeServiceTest, HomeControllerTest and the Real* tests.
 */
@SpringBootTest(properties = {"jwt.secret=" + TestTokens.SECRET, "jwt.issuer=" + TestTokens.ISSUER})
@AutoConfigureMockMvc
@ActiveProfiles("memory")
class SpecConformanceTest {

    private static final String CORRELATION_ID = "5b9e1c1e-6f0a-4d6b-9f55-6c0f1f3a7d11";

    @Autowired
    private MockMvc mvc;

    private static String auth() {
        return "Bearer " + TestTokens.profileToken("prof_9921");
    }

    @Test
    void theHomeHasExactlyTheFieldsOfTheSpec() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, auth()))
                .andExpect(status().isOk()).andReturn();

        var fields = new tools.jackson.databind.json.JsonMapper().readTree(result.getResponse().getContentAsString())
                .propertyNames().stream().collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("profile_id", "generated_at", "source", "categories", "continue_watching", "recommended"), fields);
    }

    @Test
    void theContentTypeHeaderIsJsonWithTheUtf8CharsetOfTheSpecInSuccessAndInErrors() throws Exception {
        for (String authorization : new String[]{auth(), "Bearer garbage"}) {
            MvcResult result = mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, authorization)).andReturn();

            assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader(HttpHeaders.CONTENT_TYPE));
        }
    }

    @Test
    void generatedAtIsAnIso8601UtcTimestamp() throws Exception {
        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, auth()))
                .andExpect(jsonPath("$.generated_at").value(org.hamcrest.Matchers.matchesPattern(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z")));
    }

    @Test
    void errorsRaisedByMvcItselfUseTheStandardBodyToo() throws Exception {
        mvc.perform(post("/api/v1/home").header(HttpHeaders.AUTHORIZATION, auth()).header("X-Correlation-ID", CORRELATION_ID))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").exists())
                .andExpect(jsonPath("$.error.message").exists())
                .andExpect(jsonPath("$.error.correlation_id").value(CORRELATION_ID));

        mvc.perform(get("/api/v1/nothing-here").header(HttpHeaders.AUTHORIZATION, auth()).header("X-Correlation-ID", CORRELATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.correlation_id").value(CORRELATION_ID));
    }

    @Test
    void aMalformedAuthorizationHeaderIs401WithTheStandardBody() throws Exception {
        for (String header : new String[]{"Basic dXNlcjpwYXNz", "Bearer", "Bearer ", "token-without-scheme"}) {
            mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, header).header("X-Correlation-ID", CORRELATION_ID))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.error.correlation_id").value(CORRELATION_ID));
        }
    }

    @Test
    void theLivenessProbeIsPublicAndDoesNotDependOnRedisOrMongoDb() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
