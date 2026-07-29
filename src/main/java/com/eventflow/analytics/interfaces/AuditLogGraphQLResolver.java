package com.eventflow.analytics.interfaces;

import com.eventflow.analytics.application.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * GraphQL resolver for audit log queries.
 * Provides access to immutable audit logs for compliance and security auditing.
 */
@Controller
public class AuditLogGraphQLResolver {

    private static final Logger log = LoggerFactory.getLogger(AuditLogGraphQLResolver.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogGraphQLResolver(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @QueryMapping
    public List<AuditLogPayload> auditLogs(@Argument String userId,
                                            @Argument String workspaceId,
                                            @Argument String action,
                                            @Argument String startDate,
                                            @Argument String endDate,
                                            @Argument int first,
                                            @Argument int offset) {
        log.info("Audit logs query: userId={}, workspaceId={}, action={}", userId, workspaceId, action);

        List<com.eventflow.analytics.domain.AuditLog> logs;

        if (action != null && !action.isBlank()) {
            logs = auditLogRepository.findByAction(action, first);
        } else if (userId != null && !userId.isBlank()) {
            logs = auditLogRepository.findByUserId(UUID.fromString(userId), first);
        } else if (workspaceId != null && !workspaceId.isBlank()) {
            logs = auditLogRepository.findByWorkspaceId(UUID.fromString(workspaceId), first, offset);
        } else if (startDate != null && endDate != null) {
            logs = auditLogRepository.findByDateRange(
                Instant.parse(startDate), Instant.parse(endDate), first, offset);
        } else {
            // Default: return last 50 audit logs
            logs = auditLogRepository.findByWorkspaceId(
                UUID.fromString("00000000-0000-0000-0000-000000000000"), 50, 0);
        }

        return logs.stream()
            .map(logEntry -> new AuditLogPayload(
                logEntry.getId().toString(),
                logEntry.getUserId().toString(),
                logEntry.getWorkspaceId() != null ? logEntry.getWorkspaceId().toString() : null,
                logEntry.getAction(),
                logEntry.getEntityType(),
                logEntry.getEntityId(),
                logEntry.getIpAddress(),
                logEntry.getCreatedAt().toString()
            ))
            .toList();
    }

    public record AuditLogPayload(
        String id,
        String userId,
        String workspaceId,
        String action,
        String entityType,
        String entityId,
        String ipAddress,
        String createdAt
    ) {}
}