package com.streamhub.auth_service.application.ports.out;

import com.streamhub.auth_service.domain.model.Account;

import java.util.Optional;

public interface AccountRepositoryPort {

    Optional<Account> findByEmail(String email);

    Account save(Account account);
}
