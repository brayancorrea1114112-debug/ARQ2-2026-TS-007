package com.streamhub.telemetry_service.domain.exception;

/** The event body breaks the contract of spec.md 3.5 or 3.6. */
public class InvalidEventException extends BusinessException {

    public InvalidEventException(String message) {
        super("VALIDATION_ERROR", message);
    }
}
