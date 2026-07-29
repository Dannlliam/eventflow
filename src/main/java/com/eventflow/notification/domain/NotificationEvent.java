package com.eventflow.notification.domain;

import com.eventflow.common.domain.BaseEntity;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a single event in a notification's lifecycle timeline.
 * This is an append-only event record for audit and traceability.
 */
public class NotificationEvent extends BaseEntity {

    private final UUID notificationId;
    private final NotificationEventType eventType;
    private final String providerResponse;
    private final String errorMessage;

    public NotificationEvent(UUID notificationId, NotificationEventType eventType,
                             String providerResponse, String errorMessage) {
        super();
        this.notificationId = notificationId;
        this.eventType = eventType;
        this.providerResponse = providerResponse;
        this.errorMessage = errorMessage;
    }

    public NotificationEvent(UUID id, UUID notificationId, NotificationEventType eventType,
                             String providerResponse, String errorMessage,
                             Instant createdAt, Instant updatedAt, long version) {
        super(id, createdAt, updatedAt, version);
        this.notificationId = notificationId;
        this.eventType = eventType;
        this.providerResponse = providerResponse;
        this.errorMessage = errorMessage;
    }

    public UUID getNotificationId() { return notificationId; }
    public NotificationEventType getEventType() { return eventType; }
    public String getProviderResponse() { return providerResponse; }
    public String getErrorMessage() { return errorMessage; }
}