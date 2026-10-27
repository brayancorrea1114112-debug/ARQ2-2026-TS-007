package com.streamhub.auth_service.infrastructure.adapters.out.persistence;

import com.streamhub.auth_service.application.ports.out.ProfileRepositoryPort;
import com.streamhub.auth_service.domain.model.Profile;
import com.streamhub.auth_service.infrastructure.mappers.AuthMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaProfileAdapter implements ProfileRepositoryPort {

    private final SpringDataProfileRepository repository;

    public JpaProfileAdapter(SpringDataProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Profile> findByAccountId(UUID accountId) {
        return repository.findByAccountIdOrderByCreatedAtAsc(accountId).stream().map(AuthMapper::toDomain).toList();
    }

    @Override
    public Optional<Profile> findById(String profileId) {
        return repository.findById(profileId).map(AuthMapper::toDomain);
    }

    @Override
    public Profile save(Profile profile) {
        return AuthMapper.toDomain(repository.save(AuthMapper.toEntity(profile)));
    }
}
