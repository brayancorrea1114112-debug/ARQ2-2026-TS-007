package com.streamhub.catalog_service.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** spec.md Feature 2: Redis and MongoDB both down give 503 with the standard body. Nothing listens on port 1. */
@SpringBootTest(properties = {
        "jwt.secret=" + TestTokens.SECRET, "jwt.issuer=" + TestTokens.ISSUER,
        "spring.data.redis.host=localhost", "spring.data.redis.port=1",
        "spring.mongodb.uri=mongodb://localhost:1/streamhub", "catalog.mongodb.timeout=300ms"})
@AutoConfigureMockMvc
class RealAdaptersUnavailableTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void whenRedisAndMongoDbAreDownTheHomeIs503WithTheStandardBody() throws Exception {
        mvc.perform(get("/api/v1/home")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.profileToken("prof_9921"))
                        .header("X-Correlation-ID", "5b9e1c1e-6f0a-4d6b-9f55-6c0f1f3a7d11"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.correlation_id").value("5b9e1c1e-6f0a-4d6b-9f55-6c0f1f3a7d11"));
    }
}
