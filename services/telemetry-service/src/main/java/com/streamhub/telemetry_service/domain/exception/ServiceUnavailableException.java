package com.streamhub.telemetry_service.domain.exception;

/**
 * The message broker did not confirm the publication. The client may retry with the same
 * event_id (spec.md 3.5 and 3.6).
 */
public class ServiceUnavailableException extends BusinessException {

    public ServiceUnavailableException(String dependency) {
        super("SERVICE_UNAVAILABLE", dependency + " is unavailable.");
    }

    public ServiceUnavailableException(String dependency, Throwable cause) {
        this(dependency);
        initCause(cause);
    }
}
