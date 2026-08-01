package com.eventflow.notification.application;

import com.eventflow.notification.domain.NotificationEvent;
import java.util.List;
import java.util.UUID;

/**
 * Port for NotificationEvent persistence operations.
 */
public interface NotificationEventRepository {
    NotificationEvent save(NotificationEvent event);
    List<NotificationEvent> findByNotificationId(UUID notificationId);
}