package com.kazi.kazi.apikey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_key")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 12, unique = true)
    private String keyPrefix;

    @Column(nullable = false, length = 64)
    private String keyHash;

    @Column(nullable = false, length = 100)
    private String ownerLabel;

    @Column(length = 254)
    private String ownerEmail;

    @Column(nullable = false, length = 200)
    private String scopes;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    private Instant lastUsedAt;

    protected ApiKey() {
    }

    ApiKey(
        String keyPrefix,
        String keyHash,
        String ownerLabel,
        String ownerEmail,
        String scopes,
        Instant createdAt,
        Instant expiresAt
    ) {
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.ownerLabel = ownerLabel;
        this.ownerEmail = ownerEmail;
        this.scopes = scopes;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    void markUsed(Instant now) {
        lastUsedAt = now;
    }

    public UUID getId() {
        return id;
    }

    String getKeyPrefix() {
        return keyPrefix;
    }

    String getKeyHash() {
        return keyHash;
    }

    String getOwnerLabel() {
        return ownerLabel;
    }

    String getOwnerEmail() {
        return ownerEmail;
    }

    String getScopes() {
        return scopes;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    Instant getRevokedAt() {
        return revokedAt;
    }

    Instant getLastUsedAt() {
        return lastUsedAt;
    }
}
