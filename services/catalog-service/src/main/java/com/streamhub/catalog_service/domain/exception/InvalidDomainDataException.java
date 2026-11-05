package com.streamhub.catalog_service.domain.exception;

/** Thrown when a domain object would be created in a state that breaks one of its rules. */
public class InvalidDomainDataException extends BusinessException {

    public InvalidDomainDataException(String message) {
        super("INVALID_DOMAIN_DATA", message);
    }
}
