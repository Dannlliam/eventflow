package com.eventflow.analytics.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the audit_logs table.
 */
@Repository
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    List<AuditLogJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AuditLogJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<AuditLogJpaEntity> findByActionOrderByCreatedAtDesc(String action);

    List<AuditLogJpaEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant start, Instant end);
}