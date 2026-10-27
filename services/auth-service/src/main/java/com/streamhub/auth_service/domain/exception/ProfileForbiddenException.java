package com.streamhub.auth_service.domain.exception;

public class ProfileForbiddenException extends BusinessException {

    public ProfileForbiddenException() {
        super("PROFILE_FORBIDDEN", "The profile belongs to another account.");
    }
}
