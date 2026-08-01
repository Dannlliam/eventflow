package com.eventflow.notification.domain.events;

import com.eventflow.common.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a dispatch result is received from a provider.
 */
public record DispatchResultEvent(
    String eventId,
    UUID notificationId,
    UUID workspaceId,
    String providerType,
    boolean success,
    boolean transientFailure,
    int httpStatusCode,
    String providerResponse,
    String errorMessage,
    long timestamp
) implements DomainEvent {

    public static DispatchResultEvent success(UUID notificationId, UUID workspaceId,
                                               String providerType, int httpStatusCode,
                                               String providerResponse) {
        return new DispatchResultEvent(
            "evt_" + UUID.randomUUID().toString().substring(0, 15),
            notificationId, workspaceId, providerType, true, false,
            httpStatusCode, providerResponse, null,
            Instant.now().toEpochMilli()
        );
    }

    public static DispatchResultEvent transientFailure(UUID notificationId, UUID workspaceId,
                                                        String providerType, int httpStatusCode,
                                                        String errorMessage) {
        return new DispatchResultEvent(
            "evt_" + UUID.randomUUID().toString().substring(0, 15),
            notificationId, workspaceId, providerType, false, true,
            httpStatusCode, null, errorMessage,
            Instant.now().toEpochMilli()
        );
    }

    public static DispatchResultEvent permanentFailure(UUID notificationId, UUID workspaceId,
                                                        String providerType, int httpStatusCode,
                                                        String errorMessage) {
        return new DispatchResultEvent(
            "evt_" + UUID.randomUUID().toString().substring(0, 15),
            notificationId, workspaceId, providerType, false, false,
            httpStatusCode, null, errorMessage,
            Instant.now().toEpochMilli()
        );
    }

    @Override
    public String getEventId() { return eventId; }

    @Override
    public String getEventType() { return "dispatch.result"; }

    @Override
    public long getTimestamp() { return timestamp; }
}