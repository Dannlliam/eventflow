package com.eventflow.analytics.infrastructure;

import com.eventflow.analytics.application.AuditLogRepository;
import com.eventflow.analytics.domain.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Asynchronously processes audit events published by the AuditingAspect.
 * This ensures that audit logging does not block the main transaction.
 */
@Component
public class AuditEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(AuditEventProcessor.class);

    private final AuditLogRepository auditLogRepository;

    public AuditEventProcessor(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    @EventListener
    public void handleAuditEvent(AuditingAspect.AuditEvent event) {
        try {
            auditLogRepository.save(event.auditLog());
            log.debug("Audit log persisted asynchronously: action={}", event.auditLog().getAction());
        } catch (Exception e) {
            log.error("Failed to persist audit log asynchronously", e);
        }
    }
}