package com.eventflow.analytics.infrastructure;

import com.eventflow.analytics.application.AuditLogRepository;
import com.eventflow.analytics.domain.AuditLog;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA implementation of the AuditLogRepository port.
 * The audit_logs table is append-only; no update or delete operations are exposed.
 */
@Repository
@Transactional
public class JpaAuditLogRepository implements AuditLogRepository {

    private final SpringDataAuditLogRepository springDataRepository;

    public JpaAuditLogRepository(SpringDataAuditLogRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity entity = toJpaEntity(auditLog);
        AuditLogJpaEntity saved = springDataRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<AuditLog> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<AuditLog> findByUserId(UUID userId, int limit) {
        return springDataRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .limit(limit)
            .map(this::toDomainEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findByWorkspaceId(UUID workspaceId, int limit, int offset) {
        return springDataRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
            .skip(offset)
            .limit(limit)
            .map(this::toDomainEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findByAction(String action, int limit) {
        return springDataRepository.findByActionOrderByCreatedAtDesc(action).stream()
            .limit(limit)
            .map(this::toDomainEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findByDateRange(Instant start, Instant end, int limit, int offset) {
        return springDataRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end).stream()
            .skip(offset)
            .limit(limit)
            .map(this::toDomainEntity)
            .collect(Collectors.toList());
    }

    private AuditLogJpaEntity toJpaEntity(AuditLog domain) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setWorkspaceId(domain.getWorkspaceId());
        entity.setAction(domain.getAction());
        entity.setEntityType(domain.getEntityType());
        entity.setEntityId(domain.getEntityId());
        entity.setChangesJson(domain.getChangesJson());
        entity.setIpAddress(domain.getIpAddress());
        entity.setUserAgent(domain.getUserAgent());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    private AuditLog toDomainEntity(AuditLogJpaEntity entity) {
        return new AuditLog(
            entity.getId(),
            entity.getUserId(),
            entity.getWorkspaceId(),
            entity.getAction(),
            entity.getEntityType(),
            entity.getEntityId(),
            entity.getChangesJson(),
            entity.getIpAddress(),
            entity.getUserAgent(),
            entity.getCreatedAt(),
            entity.getCreatedAt(),
            0
        );
    }
}