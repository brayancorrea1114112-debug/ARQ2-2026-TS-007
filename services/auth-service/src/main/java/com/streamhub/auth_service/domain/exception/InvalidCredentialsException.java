package com.streamhub.auth_service.domain.exception;

public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "The email or the password is incorrect.");
    }
}
