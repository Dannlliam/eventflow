package com.eventflow.provider.domain;

import com.eventflow.common.domain.BaseEntity;
import com.eventflow.common.domain.Channel;
import com.eventflow.common.domain.DomainValidationException;
import java.time.Instant;
import java.util.*;

/**
 * Aggregate Root for Provider configuration.
 * Stores the configuration for third-party notification providers.
 */
public class Provider extends BaseEntity {

    private final UUID workspaceId;
    private final String name;
    private final ProviderType providerType;
    private final Channel channel;
    private final boolean isPrimary;
    private boolean enabled;
    private int rateLimit;
    private int rateLimitDurationSeconds;
    private final Map<String, String> credentials;
    private final Map<String, String> settings;

    public Provider(UUID workspaceId, String name, ProviderType providerType, Channel channel,
                    boolean isPrimary, int rateLimit, int rateLimitDurationSeconds,
                    Map<String, String> credentials, Map<String, String> settings) {
        super();
        this.workspaceId = Objects.requireNonNull(workspaceId);
        this.name = Objects.requireNonNull(name);
        this.providerType = Objects.requireNonNull(providerType);
        this.channel = Objects.requireNonNull(channel);
        this.isPrimary = isPrimary;
        this.enabled = true;
        this.rateLimit = rateLimit;
        this.rateLimitDurationSeconds = rateLimitDurationSeconds;
        this.credentials = credentials != null ? Collections.unmodifiableMap(new LinkedHashMap<>(credentials)) : Map.of();
        this.settings = settings != null ? Collections.unmodifiableMap(new LinkedHashMap<>(settings)) : Map.of();
    }

    public Provider(UUID id, UUID workspaceId, String name, ProviderType providerType,
                    Channel channel, boolean isPrimary, boolean enabled,
                    int rateLimit, int rateLimitDurationSeconds,
                    Map<String, String> credentials, Map<String, String> settings,
                    Instant createdAt, Instant updatedAt, long version) {
        super(id, createdAt, updatedAt, version);
        this.workspaceId = workspaceId;
        this.name = name;
        this.providerType = providerType;
        this.channel = channel;
        this.isPrimary = isPrimary;
        this.enabled = enabled;
        this.rateLimit = rateLimit;
        this.rateLimitDurationSeconds = rateLimitDurationSeconds;
        this.credentials = credentials;
        this.settings = settings;
    }

    public void enable() {
        this.enabled = true;
        markUpdated();
    }

    public void disable() {
        this.enabled = false;
        markUpdated();
    }

    public void updateRateLimit(int rateLimit, int durationSeconds) {
        if (rateLimit <= 0) {
            throw new DomainValidationException("INVALID_RATE_LIMIT", "Rate limit must be positive");
        }
        this.rateLimit = rateLimit;
        this.rateLimitDurationSeconds = durationSeconds;
        markUpdated();
    }

    public UUID getWorkspaceId() { return workspaceId; }
    public String getName() { return name; }
    public ProviderType getProviderType() { return providerType; }
    public Channel getChannel() { return channel; }
    public boolean isPrimary() { return isPrimary; }
    public boolean isEnabled() { return enabled; }
    public int getRateLimit() { return rateLimit; }
    public int getRateLimitDurationSeconds() { return rateLimitDurationSeconds; }
    public Map<String, String> getCredentials() { return credentials; }
    public Map<String, String> getSettings() { return settings; }
}