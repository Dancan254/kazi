package com.kazi.kazi.apikey;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

record ApiKeyView(
    UUID id,
    String keyPrefix,
    String ownerLabel,
    String ownerEmail,
    Set<String> scopes,
    Instant createdAt,
    Instant expiresAt,
    Instant revokedAt,
    Instant lastUsedAt
) {

    static ApiKeyView from(ApiKey key) {
        return new ApiKeyView(
            key.getId(),
            key.getKeyPrefix(),
            key.getOwnerLabel(),
            key.getOwnerEmail(),
            Set.copyOf(List.of(key.getScopes().split(","))),
            key.getCreatedAt(),
            key.getExpiresAt(),
            key.getRevokedAt(),
            key.getLastUsedAt()
        );
    }
}
