package com.streamhub.telemetry_service.domain.model;

/**
 * Tracing metadata of the HTTP request. Kong creates both headers when the client does
 * not send them (spec.md 1.3 and Feature 5).
 */
public record EventContext(String correlationId, String traceparent) {
}
