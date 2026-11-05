package com.streamhub.catalog_service.domain.exception;

/**
 * A dependency of the service (cache or catalog database) cannot be reached. The adapters throw it
 * when their technology fails, so the use cases never see Redis or MongoDB errors.
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
