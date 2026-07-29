package com.eventflow.identity.domain;

import com.eventflow.common.domain.BaseEntity;
import com.eventflow.common.domain.DomainValidationException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root for API Key entity.
 * API keys are used for machine-to-machine authentication.
 * The raw key is only shown upon creation; only the SHA-256 hash is stored.
 */
public class ApiKey extends BaseEntity {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int KEY_LENGTH = 32;

    private final UUID workspaceId;
    private final String keyPrefix;
    private final String keyHash;
    private final String description;
    private boolean active;
    private Instant lastUsedAt;

    public ApiKey(UUID workspaceId, String description) {
        super();
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.description = description;
        this.active = true;
        byte[] keyBytes = new byte[KEY_LENGTH];
        SECURE_RANDOM.nextBytes(keyBytes);
        String rawKey = "ef_live_" + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        this.keyPrefix = rawKey.substring(0, 12); // "ef_live_XXXX"
        this.keyHash = hashKey(rawKey);
    }

    public ApiKey(UUID id, UUID workspaceId, String keyPrefix, String keyHash,
                  String description, boolean active, Instant createdAt,
                  Instant updatedAt, long version, Instant lastUsedAt) {
        super(id, createdAt, updatedAt, version);
        this.workspaceId = workspaceId;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.description = description;
        this.active = active;
        this.lastUsedAt = lastUsedAt;
    }

    public static String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
        markUpdated();
    }

    public void recordUsage() {
        this.lastUsedAt = Instant.now();
        markUpdated();
    }

    public boolean matchesHash(String rawKey) {
        return this.keyHash.equals(hashKey(rawKey));
    }

    public java.util.Optional<Instant> getLastUsedAt() {
        return java.util.Optional.ofNullable(lastUsedAt);
    }
}