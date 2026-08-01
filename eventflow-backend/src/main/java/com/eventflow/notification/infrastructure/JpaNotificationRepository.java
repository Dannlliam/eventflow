package com.eventflow.notification.infrastructure;

import com.eventflow.notification.domain.*;
import com.eventflow.notification.application.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

/**
 * JPA implementation of the NotificationRepository port.
 * Maps between domain aggregates and JPA entities.
 */
@Repository
@Transactional
public class JpaNotificationRepository implements NotificationRepository {

    private final SpringDataNotificationRepository springDataRepository;
    private final ObjectMapper objectMapper;

    public JpaNotificationRepository(SpringDataNotificationRepository springDataRepository,
                                     ObjectMapper objectMapper) {
        this.springDataRepository = springDataRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = toJpaEntity(notification);
        NotificationJpaEntity saved = springDataRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public Optional<Notification> findByIdAndWorkspaceId(UUID id, UUID workspaceId) {
        return springDataRepository.findByIdAndWorkspaceId(id, workspaceId).map(this::toDomainEntity);
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(UUID workspaceId, String idempotencyKey) {
        return springDataRepository.findByIdempotencyKey(workspaceId, idempotencyKey)
            .map(this::toDomainEntity);
    }

    @Override
    public List<Notification> findByStatusAndNextRetryAtBefore(NotificationStatus status,
                                                                Instant before, int limit) {
        return springDataRepository.findByStatusAndNextRetryAtBefore(
            status.name(), before, PageRequest.of(0, limit)
        ).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public long countByStatus(NotificationStatus status) {
        return springDataRepository.countByStatus(status.name());
    }

    @Override
    public void updateStatus(UUID id, NotificationStatus status) {
        springDataRepository.updateStatus(id, status.name());
    }

    @Override
    public List<Notification> findAll(UUID workspaceId, String status, String channel,
                                       int limit, int offset) {
        return springDataRepository.findByWorkspaceIdOrderByCreatedAtDesc(
            workspaceId, PageRequest.of(offset / limit, limit)
        ).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Notification> findAllWithDateRange(UUID workspaceId, String status, String channel,
                                                    Instant startDate, Instant endDate,
                                                    int limit, int offset) {
        if (status != null && !status.isBlank()) {
            return springDataRepository
                .findByWorkspaceIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                    workspaceId, status, startDate, endDate, PageRequest.of(offset / limit, limit)
                ).stream().map(this::toDomainEntity).toList();
        }
        return springDataRepository
            .findByWorkspaceIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                workspaceId, startDate, endDate, PageRequest.of(offset / limit, limit)
            ).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public long countByWorkspaceId(UUID workspaceId) {
        return springDataRepository.countByWorkspaceId(workspaceId);
    }

    private NotificationJpaEntity toJpaEntity(Notification domain) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(domain.getId());
        entity.setWorkspaceId(domain.getWorkspaceId());
        entity.setChannel(domain.getChannel().name());
        try {
            entity.setRecipient(objectMapper.writeValueAsString(domain.getRecipient().toMap()));
            entity.setPayload(objectMapper.writeValueAsString(domain.getPayload()));
            entity.setMetadata(objectMapper.writeValueAsString(domain.getMetadata()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize notification fields", e);
        }
        entity.setTemplateSlug(domain.getTemplateSlug().orElse(null));
        entity.setStatus(domain.getStatus().name());
        entity.setProviderId(domain.getProviderId().orElse(null));
        entity.setIdempotencyKey(domain.getIdempotencyKey().orElse(null));
        entity.setAttemptCount(domain.getAttemptCount());
        entity.setNextRetryAt(domain.getNextRetryAt().orElse(null));
        entity.setSentAt(domain.getSentAt().orElse(null));
        entity.setDeliveredAt(domain.getDeliveredAt().orElse(null));
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
        return entity;
    }

    @SuppressWarnings("unchecked")
    private Notification toDomainEntity(NotificationJpaEntity entity) {
        Recipient recipient;
        try {
            Map<String, Object> recipientMap = objectMapper.readValue(entity.getRecipient(), Map.class);
            recipient = new Recipient(
                (String) recipientMap.get("email"),
                (String) recipientMap.get("phone"),
                (String) recipientMap.get("deviceToken"),
                (String) recipientMap.get("webhookUrl")
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize notification", e);
        }

        Map<String, String> payload = parseJsonMap(entity.getPayload());
        Map<String, String> metadata = parseJsonMap(entity.getMetadata());

        return Notification.reconstitute(
            entity.getId(), entity.getWorkspaceId(),
            com.eventflow.common.domain.Channel.fromString(entity.getChannel()),
            recipient, entity.getTemplateSlug(), payload, metadata,
            NotificationStatus.valueOf(entity.getStatus()),
            entity.getProviderId(), entity.getIdempotencyKey(),
            entity.getAttemptCount(), entity.getNextRetryAt(),
            entity.getSentAt(), entity.getDeliveredAt(),
            entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}