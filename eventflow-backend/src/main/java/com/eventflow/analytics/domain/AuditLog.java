package com.eventflow.analytics.domain;

import com.eventflow.common.domain.BaseEntity;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an audit log entry.
 * Records administrative actions for compliance and security auditing.
 * This table is append-only; entries are never modified or deleted.
 */
public class AuditLog extends BaseEntity {

    private final UUID userId;
    private final UUID workspaceId;
    private final String action;
    private final String entityType;
    private final String entityId;
    private final String changesJson;
    private final String ipAddress;
    private final String userAgent;

    public AuditLog(UUID userId, UUID workspaceId, String action,
                    String entityType, String entityId,
                    String changesJson, String ipAddress, String userAgent) {
        super();
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.changesJson = changesJson;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public AuditLog(UUID id, UUID userId, UUID workspaceId, String action,
                    String entityType, String entityId, String changesJson,
                    String ipAddress, String userAgent,
                    Instant createdAt, Instant updatedAt, long version) {
        super(id, createdAt, updatedAt, version);
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.changesJson = changesJson;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public UUID getUserId() { return userId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getChangesJson() { return changesJson; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
}