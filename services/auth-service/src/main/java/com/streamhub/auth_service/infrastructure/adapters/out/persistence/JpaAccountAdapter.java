package com.streamhub.auth_service.infrastructure.adapters.out.persistence;

import com.streamhub.auth_service.application.ports.out.AccountRepositoryPort;
import com.streamhub.auth_service.domain.model.Account;
import com.streamhub.auth_service.infrastructure.mappers.AuthMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaAccountAdapter implements AccountRepositoryPort {

    private final SpringDataAccountRepository repository;

    public JpaAccountAdapter(SpringDataAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Account> findByEmail(String email) {
        return repository.findByEmail(email).map(AuthMapper::toDomain);
    }

    @Override
    public Account save(Account account) {
        return AuthMapper.toDomain(repository.save(AuthMapper.toEntity(account)));
    }
}
