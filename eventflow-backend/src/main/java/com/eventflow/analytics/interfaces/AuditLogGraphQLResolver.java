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
    public AuditLogConnectionPayload auditLogs(@Argument AuditLogFilterInput filter,
                                               @Argument int first,
                                               @Argument String after) {
        log.info("Audit logs query: filter={}, first={}, after={}", filter, first, after);

        int offset = 0;
        if (after != null && !after.isBlank()) {
            try {
                offset = Integer.parseInt(new String(java.util.Base64.getDecoder().decode(after)));
            } catch (Exception e) {
                log.warn("Invalid cursor: {}", after);
            }
        }

        List<com.eventflow.analytics.domain.AuditLog> logs;
        
        if (filter != null && filter.action() != null && !filter.action().isBlank()) {
            logs = auditLogRepository.findByAction(filter.action(), first);
        } else if (filter != null && filter.userId() != null && !filter.userId().isBlank()) {
            logs = auditLogRepository.findByUserId(UUID.fromString(filter.userId()), first);
        } else {
            // Default: return recent audit logs
            logs = auditLogRepository.findByWorkspaceId(
                UUID.fromString("00000000-0000-0000-0000-000000000000"), first, offset);
        }

        List<AuditLogPayload> items = logs.stream()
            .map(logEntry -> new AuditLogPayload(
                logEntry.getId().toString(),
                logEntry.getUserId().toString(),
                logEntry.getAction(),
                logEntry.getEntityType(),
                logEntry.getEntityId(),
                null, // changes - TODO: implement change tracking
                logEntry.getIpAddress(),
                logEntry.getCreatedAt().toString()
            ))
            .toList();

        boolean hasNextPage = items.size() == first;
        String endCursor = items.isEmpty() ? null : 
            java.util.Base64.getEncoder().encodeToString(String.valueOf(offset + items.size()).getBytes());

        List<AuditLogEdgePayload> edges = items.stream()
            .map(log -> new AuditLogEdgePayload(log, log.id()))
            .toList();

        return new AuditLogConnectionPayload(
            edges,
            new PageInfoPayload(hasNextPage, endCursor, (long) items.size())
        );
    }

    public record AuditLogConnectionPayload(
        List<AuditLogEdgePayload> edges,
        PageInfoPayload pageInfo
    ) {}

    public record AuditLogEdgePayload(
        AuditLogPayload node,
        String cursor
    ) {}

    public record PageInfoPayload(
        boolean hasNextPage,
        String endCursor,
        long totalCount
    ) {}

    public record AuditLogFilterInput(
        String userId,
        String action,
        String entityType,
        String dateFrom,
        String dateTo
    ) {}

    public record AuditLogPayload(
        String id,
        String userId,
        String action,
        String entityType,
        String entityId,
        Object changes,
        String ipAddress,
        String createdAt
    ) {}
}