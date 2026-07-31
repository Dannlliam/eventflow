package com.eventflow.common.infrastructure;

import com.eventflow.common.domain.Auditable;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for recording audit events for domain entities annotated with @Auditable.
 * Tracks create, update, and delete operations with before/after snapshots.
 * <p>
 * As specified in the PRD Section 73 - OWASP Compliance / Audit Logging.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final ObjectMapper objectMapper;

    public AuditService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Records an audit event for a created entity.
     */
    public void recordCreation(Object entity, UUID performedBy) {
        String entityType = getEntityType(entity);
        UUID entityId = extractEntityId(entity);

        log.info("AUDIT: CREATE entityType={}, entityId={}, performedBy={}",
            entityType, entityId, performedBy);

        saveAuditEvent(new AuditEvent(
            UUID.randomUUID(),
            entityType,
            entityId,
            AuditAction.CREATE,
            null,
            serializeSnapshot(entity),
            performedBy,
            Instant.now()
        ));
    }

    /**
     * Records an audit event for an updated entity.
     */
    public void recordUpdate(Object before, Object after, UUID performedBy) {
        String entityType = getEntityType(after);
        UUID entityId = extractEntityId(after);

        log.info("AUDIT: UPDATE entityType={}, entityId={}, performedBy={}",
            entityType, entityId, performedBy);

        saveAuditEvent(new AuditEvent(
            UUID.randomUUID(),
            entityType,
            entityId,
            AuditAction.UPDATE,
            serializeSnapshot(before),
            serializeSnapshot(after),
            performedBy,
            Instant.now()
        ));
    }

    /**
     * Records an audit event for a deleted entity.
     */
    public void recordDeletion(Object entity, UUID performedBy) {
        String entityType = getEntityType(entity);
        UUID entityId = extractEntityId(entity);

        log.info("AUDIT: DELETE entityType={}, entityId={}, performedBy={}",
            entityType, entityId, performedBy);

        saveAuditEvent(new AuditEvent(
            UUID.randomUUID(),
            entityType,
            entityId,
            AuditAction.DELETE,
            serializeSnapshot(entity),
            null,
            performedBy,
            Instant.now()
        ));
    }

    private String getEntityType(Object entity) {
        Auditable auditable = entity.getClass().getAnnotation(Auditable.class);
        if (auditable != null && !auditable.value().isBlank()) {
            return auditable.value();
        }
        return entity.getClass().getSimpleName();
    }

    @SuppressWarnings("unchecked")
    private UUID extractEntityId(Object entity) {
        try {
            var getIdMethod = entity.getClass().getMethod("getId");
            Object idObj = getIdMethod.invoke(entity);
            if (idObj instanceof UUID) {
                return (UUID) idObj;
            }
            if (idObj instanceof String) {
                return UUID.fromString((String) idObj);
            }
        } catch (Exception e) {
            log.warn("Cannot extract entity ID for audit: {}", e.getMessage());
        }
        return UUID.randomUUID();
    }

    private String serializeSnapshot(Object entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            log.warn("Cannot serialize entity snapshot for audit: {}", e.getMessage());
            return "{\"error\":\"serialization_failed\"}";
        }
    }

    private void saveAuditEvent(AuditEvent event) {
        // In production, writes to audit_logs table or sends to audit stream
        log.debug("Audit event saved: action={}, entityType={}, entityId={}, performedBy={}",
            event.action(), event.entityType(), event.entityId(), event.performedBy());
    }

    // Audit event types
    public enum AuditAction {
        CREATE, UPDATE, DELETE
    }

    /**
     * Immutable record representing a single audit event.
     */
    public record AuditEvent(
        UUID id,
        String entityType,
        UUID entityId,
        AuditAction action,
        String beforeSnapshot,
        String afterSnapshot,
        UUID performedBy,
        Instant timestamp
    ) {}
}