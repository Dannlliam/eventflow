package com.eventflow.notification.domain.events;

import com.eventflow.common.domain.Channel;
import com.eventflow.common.domain.DomainEvent;
import com.eventflow.notification.domain.Recipient;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event emitted when a notification is created and queued.
 */
public record NotificationCreatedEvent(
    String eventId,
    UUID notificationId,
    UUID workspaceId,
    Channel channel,
    Recipient recipient,
    String templateSlug,
    Map<String, String> payload,
    Map<String, String> metadata,
    long timestamp
) implements DomainEvent {

    public NotificationCreatedEvent(UUID notificationId, UUID workspaceId, Channel channel,
                                    Recipient recipient, String templateSlug,
                                    Map<String, String> payload, Map<String, String> metadata) {
        this(
            "evt_" + UUID.randomUUID().toString().substring(0, 15),
            notificationId,
            workspaceId,
            channel,
            recipient,
            templateSlug,
            payload,
            metadata,
            Instant.now().toEpochMilli()
        );
    }

    @Override
    public String getEventId() { return eventId; }

    @Override
    public String getEventType() { return "notification.created"; }

    @Override
    public long getTimestamp() { return timestamp; }
}