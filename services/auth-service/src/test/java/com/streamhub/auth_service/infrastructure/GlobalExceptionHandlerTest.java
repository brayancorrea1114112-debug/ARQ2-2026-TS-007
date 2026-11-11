package com.streamhub.auth_service.infrastructure;

import com.streamhub.auth_service.infrastructure.adapters.in.rest.GlobalExceptionHandler;
import com.streamhub.auth_service.infrastructure.adapters.in.rest.dto.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void anUnreachableDatabaseIsAServiceUnavailable() {
        ResponseEntity<ApiError> response = new GlobalExceptionHandler()
                .handleDatabaseUnavailable(new CannotCreateTransactionException("connection refused"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().error().code());
    }
}
