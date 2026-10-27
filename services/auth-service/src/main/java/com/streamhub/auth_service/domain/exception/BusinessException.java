package com.streamhub.auth_service.domain.exception;

/**
 * Base class for business rule violations. The code is the stable value returned in the
 * error body defined in spec.md 1.4; the HTTP status is decided by the REST adapter.
 */
public abstract class BusinessException extends RuntimeException {

    private final String code;

    protected BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
