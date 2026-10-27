package com.streamhub.auth_service.application;

import com.streamhub.auth_service.application.ports.out.AccountRepositoryPort;
import com.streamhub.auth_service.application.ports.out.PasswordHasherPort;
import com.streamhub.auth_service.application.ports.out.ProfileRepositoryPort;
import com.streamhub.auth_service.application.ports.out.TokenIssuerPort;
import com.streamhub.auth_service.domain.model.Account;
import com.streamhub.auth_service.domain.model.Profile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory implementations of the output ports, so the use cases are tested without Spring or a database. */
final class FakePorts {

    private FakePorts() {
    }

    static class InMemoryAccounts implements AccountRepositoryPort {
        private final Map<String, Account> byEmail = new HashMap<>();

        @Override
        public Optional<Account> findByEmail(String email) {
            return Optional.ofNullable(byEmail.get(email));
        }

        @Override
        public Account save(Account account) {
            byEmail.put(account.email(), account);
            return account;
        }
    }

    static class InMemoryProfiles implements ProfileRepositoryPort {
        private final Map<String, Profile> byId = new HashMap<>();

        @Override
        public List<Profile> findByAccountId(UUID accountId) {
            return byId.values().stream().filter(profile -> profile.belongsTo(accountId)).toList();
        }

        @Override
        public Optional<Profile> findById(String profileId) {
            return Optional.ofNullable(byId.get(profileId));
        }

        @Override
        public Profile save(Profile profile) {
            byId.put(profile.id(), profile);
            return profile;
        }
    }

    /** "Hashing" is a prefix, enough to tell a right password from a wrong one. */
    static class PrefixHasher implements PasswordHasherPort {
        @Override
        public String hash(String rawPassword) {
            return "hash:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return hash(rawPassword).equals(passwordHash);
        }
    }

    static class FakeTokens implements TokenIssuerPort {
        @Override
        public IssuedToken issueAccountToken(UUID accountId) {
            return new IssuedToken("account:" + accountId, 3600);
        }

        @Override
        public IssuedToken issueProfileToken(UUID accountId, String profileId) {
            return new IssuedToken("profile:" + accountId + ":" + profileId, 3600);
        }
    }
}
