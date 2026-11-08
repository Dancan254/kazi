package com.kazi.kazi.apikey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {

    static final String JOBS_READ = "jobs:read";
    static final String METRICS_READ = "metrics:read";
    private static final int DEFAULT_EXPIRY_DAYS = 90;
    private static final Set<String> ALLOWED_SCOPES = Set.of(JOBS_READ, METRICS_READ);
    private static final Pattern KEY_FORMAT = Pattern.compile("^kazi_([0-9a-f]{12})_[0-9a-f]{48}$");
    private static final HexFormat HEX = HexFormat.of();

    private final ApiKeyRepository repository;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    ApiKeyService(ApiKeyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Optional<ApiKeyAuthentication> authenticate(String candidate) {
        if (candidate == null) {
            return Optional.empty();
        }
        var matcher = KEY_FORMAT.matcher(candidate);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return repository.findByKeyPrefix(matcher.group(1))
            .filter(key -> matches(candidate, key.getKeyHash()))
            .filter(key -> key.isActive(clock.instant()))
            .map(key -> {
                var now = clock.instant();
                if (key.getLastUsedAt() == null || key.getLastUsedAt().isBefore(now.minus(5, ChronoUnit.MINUTES))) {
                    key.markUsed(now);
                }
                return new ApiKeyAuthentication(key.getOwnerLabel(), scopes(key));
            });
    }

    @Transactional
    IssuedApiKey create(String ownerLabel, String ownerEmail, Integer expiresInDays, Set<String> requestedScopes) {
        if (ownerLabel == null || ownerLabel.isBlank()) {
            throw new IllegalArgumentException("Owner label is required");
        }
        var scopes = normalizeScopes(requestedScopes);
        var days = expiresInDays == null ? DEFAULT_EXPIRY_DAYS : expiresInDays;
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("Expiry must be between 1 and 365 days");
        }
        var prefix = newPrefix();
        while (repository.findByKeyPrefix(prefix).isPresent()) {
            prefix = newPrefix();
        }
        var secret = randomHex(24);
        var plaintext = "kazi_" + prefix + "_" + secret;
        var now = clock.instant();
        var key = new ApiKey(
            prefix,
            hash(plaintext),
            ownerLabel.trim(),
            normalizeEmail(ownerEmail),
            String.join(",", scopes),
            now,
            now.plus(days, ChronoUnit.DAYS)
        );
        return new IssuedApiKey(plaintext, ApiKeyView.from(repository.save(key)));
    }

    @Transactional(readOnly = true)
    List<ApiKeyView> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(ApiKeyView::from).toList();
    }

    @Transactional
    void revoke(UUID id) {
        var key = repository.findById(id).orElseThrow(() -> new ApiKeyNotFoundException(id));
        key.revoke(clock.instant());
    }

    @Transactional
    IssuedApiKey rotate(UUID id, Integer expiresInDays) {
        var current = repository.findById(id).orElseThrow(() -> new ApiKeyNotFoundException(id));
        current.revoke(clock.instant());
        return create(
            current.getOwnerLabel(),
            current.getOwnerEmail(),
            expiresInDays,
            scopes(current)
        );
    }

    private Set<String> normalizeScopes(Set<String> requestedScopes) {
        var scopes = requestedScopes == null || requestedScopes.isEmpty()
            ? new TreeSet<>(Set.of(JOBS_READ))
            : new TreeSet<>(requestedScopes);
        if (!ALLOWED_SCOPES.containsAll(scopes)) {
            throw new IllegalArgumentException("Supported scopes are jobs:read and metrics:read");
        }
        return scopes;
    }

    private Set<String> scopes(ApiKey key) {
        return Set.copyOf(List.of(key.getScopes().split(",")));
    }

    private String normalizeEmail(String ownerEmail) {
        return ownerEmail == null || ownerEmail.isBlank() ? null : ownerEmail.trim();
    }

    private boolean matches(String candidate, String expectedHash) {
        return MessageDigest.isEqual(
            hash(candidate).getBytes(StandardCharsets.UTF_8),
            expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String hash(String value) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String newPrefix() {
        return randomHex(6);
    }

    private String randomHex(int bytes) {
        var value = new byte[bytes];
        secureRandom.nextBytes(value);
        return HEX.formatHex(value);
    }

    record IssuedApiKey(String plaintext, ApiKeyView metadata) {
    }
}
