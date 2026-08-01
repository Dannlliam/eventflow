package com.eventflow.notification.application;

import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for Notification persistence operations.
 */
public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(UUID id);
    Optional<Notification> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    Optional<Notification> findByIdempotencyKey(UUID workspaceId, String idempotencyKey);
    List<Notification> findByStatusAndNextRetryAtBefore(NotificationStatus status, Instant before, int limit);
    long countByStatus(NotificationStatus status);
    void updateStatus(UUID id, NotificationStatus status);
    List<Notification> findAll(UUID workspaceId, String status, String channel, int limit, int offset);
    List<Notification> findAllWithDateRange(UUID workspaceId, String status, String channel,
                                              Instant startDate, Instant endDate, int limit, int offset);
    long countByWorkspaceId(UUID workspaceId);
}