package com.eventflow.provider.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the providers table.
 */
@Entity
@Table(name = "providers", schema = "eventflow")
public class ProviderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "provider_type", nullable = false, length = 50)
    private String providerType;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "rate_limit", nullable = false)
    private int rateLimit;

    @Column(name = "rate_limit_duration_seconds", nullable = false)
    private int rateLimitDurationSeconds;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String credentials;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String settings;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public ProviderJpaEntity() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getRateLimit() { return rateLimit; }
    public void setRateLimit(int rateLimit) { this.rateLimit = rateLimit; }
    public int getRateLimitDurationSeconds() { return rateLimitDurationSeconds; }
    public void setRateLimitDurationSeconds(int rateLimitDurationSeconds) { this.rateLimitDurationSeconds = rateLimitDurationSeconds; }
    public String getCredentials() { return credentials; }
    public void setCredentials(String credentials) { this.credentials = credentials; }
    public String getSettings() { return settings; }
    public void setSettings(String settings) { this.settings = settings; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}