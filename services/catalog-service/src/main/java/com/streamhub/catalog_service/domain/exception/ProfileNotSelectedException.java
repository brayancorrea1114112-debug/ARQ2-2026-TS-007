package com.streamhub.catalog_service.domain.exception;

/** The request carries no active profile (spec.md 3.4: the token has no profile_id claim). */
public class ProfileNotSelectedException extends BusinessException {

    public ProfileNotSelectedException() {
        super("PROFILE_NOT_SELECTED", "The token has no active profile. Select a profile first.");
    }
}
