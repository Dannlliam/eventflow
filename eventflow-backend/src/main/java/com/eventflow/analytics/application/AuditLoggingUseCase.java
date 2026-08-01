package com.eventflow.analytics.application;

import com.eventflow.analytics.domain.AuditLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case for recording audit log entries.
 * Provides an immutable, append-only audit trail for all administrative actions,
 * configuration changes, and security events.
 *
 * As specified in the PRD Section 59 - Audit Logging.
 */
public class AuditLoggingUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingUseCase.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLoggingUseCase(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Records an audit log entry.
     */
    public AuditLog record(UUID userId, UUID workspaceId, String action,
                           String entityType, String entityId,
                           Map<String, Object> changes,
                           String ipAddress, String userAgent) {
        String changesJson = serializeChanges(changes);

        AuditLog auditLog = new AuditLog(
            userId,
            workspaceId,
            action,
            entityType,
            entityId,
            changesJson,
            ipAddress,
            userAgent
        );

        AuditLog saved = auditLogRepository.save(auditLog);

        log.info("Audit log recorded: userId={}, action={}, entityType={}, entityId={}",
            userId, action, entityType, entityId);

        return saved;
    }

    /**
     * Records an authentication event (login, logout, failed login).
     */
    public void recordAuthEvent(UUID userId, UUID workspaceId, String action,
                                String ipAddress, String userAgent, boolean success) {
        Map<String, Object> changes = Map.of(
            "success", success,
            "timestamp", Instant.now().toString()
        );

        record(userId, workspaceId, action, "AUTH",
            userId.toString(), changes, ipAddress, userAgent);
    }

    /**
     * Records a DLQ replay event.
     */
    public void recordDlqReplay(UUID userId, UUID workspaceId, UUID notificationId, String ipAddress) {
        Map<String, Object> changes = Map.of(
            "replayedNotificationId", notificationId.toString()
        );

        record(userId, workspaceId, "DLQ_REPLAY", "NOTIFICATION",
            notificationId.toString(), changes, ipAddress, null);
    }

    /**
     * Records a provider configuration change.
     */
    public void recordProviderChange(UUID userId, UUID workspaceId, UUID providerId,
                                     String changeType, Map<String, Object> changes,
                                     String ipAddress) {
        record(userId, workspaceId, "PROVIDER_" + changeType, "PROVIDER",
            providerId.toString(), changes, ipAddress, null);
    }

    /**
     * Records a template change.
     */
    public void recordTemplateChange(UUID userId, UUID workspaceId, String templateSlug,
                                     String action, Map<String, Object> changes,
                                     String ipAddress) {
        record(userId, workspaceId, action, "TEMPLATE",
            templateSlug, changes, ipAddress, null);
    }

    /**
     * Records an API key event.
     */
    public void recordApiKeyEvent(UUID userId, UUID workspaceId, UUID keyId,
                                  String action, String ipAddress) {
        record(userId, workspaceId, action, "API_KEY",
            keyId.toString(), Map.of(), ipAddress, null);
    }

    private String serializeChanges(Map<String, Object> changes) {
        if (changes == null || changes.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit changes: {}", e.getMessage());
            return "{}";
        }
    }
}