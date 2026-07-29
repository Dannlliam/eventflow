package com.eventflow.analytics.application;

import com.eventflow.analytics.domain.AuditLog;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for AuditLog persistence operations.
 * The audit_logs table is append-only; no update or delete operations are exposed.
 */
public interface AuditLogRepository {

    /**
     * Persists an audit log entry.
     */
    AuditLog save(AuditLog auditLog);

    /**
     * Finds an audit log by ID.
     */
    Optional<AuditLog> findById(UUID id);

    /**
     * Finds audit logs by user ID, ordered by timestamp descending.
     */
    List<AuditLog> findByUserId(UUID userId, int limit);

    /**
     * Finds audit logs by workspace ID, ordered by timestamp descending.
     */
    List<AuditLog> findByWorkspaceId(UUID workspaceId, int limit, int offset);

    /**
     * Finds audit logs by action type, ordered by timestamp descending.
     */
    List<AuditLog> findByAction(String action, int limit);

    /**
     * Finds audit logs within a date range.
     */
    List<AuditLog> findByDateRange(Instant start, Instant end, int limit, int offset);
}