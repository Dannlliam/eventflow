package com.eventflow.notification.infrastructure;

import com.eventflow.notification.application.NotificationEventRepository;
import com.eventflow.notification.domain.NotificationEvent;
import com.eventflow.notification.domain.NotificationEventType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA implementation of the NotificationEventRepository port.
 */
@Repository
@Transactional
public class JpaNotificationEventRepository implements NotificationEventRepository {

    private final SpringDataNotificationEventRepository springDataRepository;

    public JpaNotificationEventRepository(SpringDataNotificationEventRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public NotificationEvent save(NotificationEvent event) {
        NotificationEventJpaEntity entity = toJpaEntity(event);
        NotificationEventJpaEntity saved = springDataRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public List<NotificationEvent> findByNotificationId(UUID notificationId) {
        return springDataRepository.findByNotificationIdOrderByCreatedAtAsc(notificationId).stream()
            .map(this::toDomainEntity)
            .toList();
    }

    private NotificationEventJpaEntity toJpaEntity(NotificationEvent domain) {
        NotificationEventJpaEntity entity = new NotificationEventJpaEntity();
        entity.setId(domain.getId());
        entity.setNotificationId(domain.getNotificationId());
        entity.setEventType(domain.getEventType().name());
        entity.setProviderResponse(domain.getProviderResponse());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
        return entity;
    }

    private NotificationEvent toDomainEntity(NotificationEventJpaEntity entity) {
        return new NotificationEvent(
            entity.getId(),
            entity.getNotificationId(),
            NotificationEventType.valueOf(entity.getEventType()),
            entity.getProviderResponse(),
            entity.getErrorMessage(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }
}