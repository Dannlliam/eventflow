package com.eventflow.notification.domain.events;

import com.eventflow.common.domain.DomainEvent;
import com.eventflow.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a notification's status changes.
 */
public record NotificationStatusChangedEvent(
    String eventId,
    UUID notificationId,
    UUID workspaceId,
    NotificationStatus previousStatus,
    NotificationStatus newStatus,
    long timestamp
) implements DomainEvent {

    public NotificationStatusChangedEvent(UUID notificationId, UUID workspaceId,
                                          NotificationStatus previousStatus,
                                          NotificationStatus newStatus) {
        this(
            "evt_" + UUID.randomUUID().toString().substring(0, 15),
            notificationId,
            workspaceId,
            previousStatus,
            newStatus,
            Instant.now().toEpochMilli()
        );
    }

    @Override
    public String getEventId() { return eventId; }

    @Override
    public String getEventType() { return "notification.status.changed"; }

    @Override
    public long getTimestamp() { return timestamp; }
}