package com.streamhub.catalog_service.infrastructure.adapters.in.rest;

import com.streamhub.catalog_service.domain.exception.BusinessException;
import com.streamhub.catalog_service.domain.exception.ProfileNotSelectedException;
import com.streamhub.catalog_service.domain.exception.ServiceUnavailableException;
import com.streamhub.catalog_service.infrastructure.adapters.in.rest.dto.ApiError;
import com.streamhub.catalog_service.infrastructure.web.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.charset.StandardCharsets;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        HttpStatus status = statusOf(ex);
        if (status.is5xxServerError()) {
            log.error("{}: {}", ex.getCode(), ex.getMessage(), ex);
        }
        return respond(status, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        // Errors raised by Spring MVC itself (404, 405...) already carry their own status.
        if (ex instanceof ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            return respond(status, "REQUEST_ERROR", "The request could not be processed.");
        }
        log.error("Unexpected error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    /** Any other business exception means the service holds data that breaks a domain rule: that is our fault, not the client's. */
    private static HttpStatus statusOf(BusinessException ex) {
        return switch (ex) {
            case ProfileNotSelectedException e -> HttpStatus.FORBIDDEN;
            case ServiceUnavailableException e -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static ResponseEntity<ApiError> respond(HttpStatusCode status, String code, String message) {
        return ResponseEntity.status(status)
                .contentType(JSON_UTF8)
                .body(ApiError.of(code, message, MDC.get(CorrelationIdFilter.MDC_KEY)));
    }
}
