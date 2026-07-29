package com.eventflow.notification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the notification_events table.
 */
@Repository
public interface SpringDataNotificationEventRepository extends JpaRepository<NotificationEventJpaEntity, UUID> {
    List<NotificationEventJpaEntity> findByNotificationIdOrderByCreatedAtAsc(UUID notificationId);
}