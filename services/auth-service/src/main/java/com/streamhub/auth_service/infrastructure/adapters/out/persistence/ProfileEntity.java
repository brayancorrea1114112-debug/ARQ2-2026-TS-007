package com.streamhub.auth_service.infrastructure.adapters.out.persistence;

import com.streamhub.auth_service.domain.model.ProfileType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    /** Read-only: it only exists so the database gets the foreign key of spec.md 2.2. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_profiles_account"))
    private AccountEntity account;

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
