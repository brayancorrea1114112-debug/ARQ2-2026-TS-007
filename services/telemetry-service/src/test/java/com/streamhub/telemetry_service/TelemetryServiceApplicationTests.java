package com.streamhub.telemetry_service;

import com.streamhub.telemetry_service.infrastructure.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "jwt.secret=" + TestTokens.SECRET,
        "jwt.issuer=" + TestTokens.ISSUER,
        "spring.rabbitmq.dynamic=false",
        "management.tracing.enabled=false"
})
@ActiveProfiles("memory")
class TelemetryServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
