package com.streamhub.auth_service.infrastructure.adapters.out.persistence;

import com.streamhub.auth_service.domain.model.ProfileType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profiles", indexes = @Index(name = "idx_profiles_account_id", columnList = "account_id"))
public class ProfileEntity {

    @Id
    @Column(name = "profile_id")
    private String id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileType type;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProfileEntity() {
    }

    public ProfileEntity(String id, UUID accountId, String name, ProfileType type, String avatarUrl, Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.name = name;
        this.type = type;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public String getName() { return name; }
    public ProfileType getType() { return type; }
    public String getAvatarUrl() { return avatarUrl; }
    public Instant getCreatedAt() { return createdAt; }
}
