package com.streamhub.auth_service.application.usecases;

import com.streamhub.auth_service.application.ports.in.ProfileUseCase;
import com.streamhub.auth_service.application.ports.out.ProfileRepositoryPort;
import com.streamhub.auth_service.application.ports.out.TokenIssuerPort;
import com.streamhub.auth_service.application.ports.out.TokenIssuerPort.IssuedToken;
import com.streamhub.auth_service.domain.exception.ProfileForbiddenException;
import com.streamhub.auth_service.domain.exception.ProfileNotFoundException;
import com.streamhub.auth_service.domain.model.Profile;

import java.util.List;
import java.util.UUID;

public class ProfileService implements ProfileUseCase {

    private final ProfileRepositoryPort profiles;
    private final TokenIssuerPort tokenIssuer;

    public ProfileService(ProfileRepositoryPort profiles, TokenIssuerPort tokenIssuer) {
        this.profiles = profiles;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public List<Profile> listProfiles(UUID accountId) {
        return profiles.findByAccountId(accountId);
    }

    @Override
    public ProfileSelection selectProfile(UUID accountId, String profileId) {
        Profile profile = profiles.findById(profileId).orElseThrow(ProfileNotFoundException::new);
        if (!profile.belongsTo(accountId)) {
            throw new ProfileForbiddenException();
        }

        IssuedToken token = tokenIssuer.issueProfileToken(accountId, profile.id());
        return new ProfileSelection(token.value(), token.expiresIn(), profile);
    }
}
