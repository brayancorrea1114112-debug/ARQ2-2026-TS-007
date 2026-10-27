package com.streamhub.auth_service.infrastructure.adapters.in.rest;

import com.streamhub.auth_service.domain.exception.BusinessException;
import com.streamhub.auth_service.domain.exception.InvalidCredentialsException;
import com.streamhub.auth_service.domain.exception.ProfileForbiddenException;
import com.streamhub.auth_service.domain.exception.ProfileNotFoundException;
import com.streamhub.auth_service.infrastructure.adapters.in.rest.dto.ApiError;
import com.streamhub.auth_service.infrastructure.web.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        return respond(statusOf(ex), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .sorted()
                .reduce((a, b) -> a + "; " + b)
                .orElse("The request body is invalid.");
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The request body is missing or is not valid JSON.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        // Errors raised by Spring MVC itself (404, 405, 415...) already carry their own status.
        if (ex instanceof ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            return respond(status, "REQUEST_ERROR", "The request could not be processed.");
        }
        log.error("Unexpected error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private static HttpStatus statusOf(BusinessException ex) {
        return switch (ex) {
            case InvalidCredentialsException e -> HttpStatus.UNAUTHORIZED;
            case ProfileNotFoundException e -> HttpStatus.NOT_FOUND;
            case ProfileForbiddenException e -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static ResponseEntity<ApiError> respond(HttpStatusCode status, String code, String message) {
        return ResponseEntity.status(status)
                .body(ApiError.of(code, message, MDC.get(CorrelationIdFilter.MDC_KEY)));
    }
}
