package com.eventflow.notification.domain.events;

import com.eventflow.common.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a dispatch is requested to a provider.
 */
public record DispatchRequestedEvent(
    String eventId,
    UUID notificationId,
    UUID workspaceId,
    String channel,
    String providerType,
    String recipient,
    String subject,
    String body,
    long timestamp
) implements DomainEvent {

    public DispatchRequestedEvent(UUID notificationId, UUID workspaceId, String channel,
                                  String providerType, String recipient, String subject, String body) {
        this(
            "evt_" + UUID.randomUUID().toString().substring(0, 15),
            notificationId,
            workspaceId,
            channel,
            providerType,
            recipient,
            subject,
            body,
            Instant.now().toEpochMilli()
        );
    }

    @Override
    public String getEventId() { return eventId; }

    @Override
    public String getEventType() { return "dispatch.requested"; }

    @Override
    public long getTimestamp() { return timestamp; }
}