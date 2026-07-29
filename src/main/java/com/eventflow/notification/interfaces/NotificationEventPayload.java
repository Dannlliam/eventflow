package com.eventflow.notification.interfaces;

/**
 * GraphQL DTO for notification event history.
 * Represents a single event in the notification lifecycle (created, dispatched, delivered, failed, etc.).
 */
public record NotificationEventPayload(
    String id,
    String eventType,
    String providerResponse,
    String errorMessage,
    String createdAt
) {}