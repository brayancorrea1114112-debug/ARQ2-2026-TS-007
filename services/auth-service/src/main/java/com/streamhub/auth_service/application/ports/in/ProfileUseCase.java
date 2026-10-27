package com.streamhub.auth_service.application.ports.in;

import com.streamhub.auth_service.domain.model.Profile;

import java.util.List;
import java.util.UUID;

public interface ProfileUseCase {

    List<Profile> listProfiles(UUID accountId);

    ProfileSelection selectProfile(UUID accountId, String profileId);

    record ProfileSelection(String accessToken, long expiresIn, Profile profile) {
    }
}
