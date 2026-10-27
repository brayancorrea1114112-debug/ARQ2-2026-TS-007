package com.streamhub.auth_service.domain.exception;

public class ProfileNotFoundException extends BusinessException {

    public ProfileNotFoundException() {
        super("PROFILE_NOT_FOUND", "The profile does not exist.");
    }
}
