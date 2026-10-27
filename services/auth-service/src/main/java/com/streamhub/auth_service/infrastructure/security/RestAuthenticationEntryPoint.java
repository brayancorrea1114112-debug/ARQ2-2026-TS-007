package com.streamhub.auth_service.infrastructure.security;

import com.streamhub.auth_service.infrastructure.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Answers 401 with the standard error body (the correlation id is always a generated or validated UUID). */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String body = "{\"error\":{\"code\":\"UNAUTHORIZED\","
                + "\"message\":\"Missing, malformed, invalid or expired token.\","
                + "\"correlation_id\":\"" + MDC.get(CorrelationIdFilter.MDC_KEY) + "\"}}";

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }
}
