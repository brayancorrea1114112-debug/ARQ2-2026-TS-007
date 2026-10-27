package com.streamhub.auth_service.infrastructure.mappers;

import com.streamhub.auth_service.domain.model.Account;
import com.streamhub.auth_service.domain.model.Profile;
import com.streamhub.auth_service.infrastructure.adapters.out.persistence.AccountEntity;
import com.streamhub.auth_service.infrastructure.adapters.out.persistence.ProfileEntity;

public final class AuthMapper {

    private AuthMapper() {
    }

    public static Account toDomain(AccountEntity entity) {
        return new Account(entity.getId(), entity.getEmail(), entity.getPasswordHash(), entity.getCreatedAt());
    }

    public static AccountEntity toEntity(Account account) {
        return new AccountEntity(account.id(), account.email(), account.passwordHash(), account.createdAt());
    }

    public static Profile toDomain(ProfileEntity entity) {
        return new Profile(entity.getId(), entity.getAccountId(), entity.getName(), entity.getType(),
                entity.getAvatarUrl(), entity.getCreatedAt());
    }

    public static ProfileEntity toEntity(Profile profile) {
        return new ProfileEntity(profile.id(), profile.accountId(), profile.name(), profile.type(),
                profile.avatarUrl(), profile.createdAt());
    }
}
