package com.streamhub.auth_service.application.ports.out;

import com.streamhub.auth_service.domain.model.Profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepositoryPort {

    List<Profile> findByAccountId(UUID accountId);

    Optional<Profile> findById(String profileId);

    Profile save(Profile profile);
}
