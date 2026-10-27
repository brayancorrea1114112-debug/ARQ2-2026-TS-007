package com.streamhub.auth_service.infrastructure.adapters.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Standard error body of spec.md 1.4. */
public record ApiError(Error error) {

    public record Error(String code, String message, @JsonProperty("correlation_id") String correlationId) {
    }

    public static ApiError of(String code, String message, String correlationId) {
        return new ApiError(new Error(code, message, correlationId));
    }
}
