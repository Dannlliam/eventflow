package com.eventflow.notification.domain;

/**
 * Types of events that can occur in a notification's lifecycle.
 */
public enum NotificationEventType {
    CREATED,
    PROCESSING,
    RATE_LIMITED,
    DISPATCHED,
    DELIVERED,
    FAILED,
    RETRY_SCHEDULED,
    DLQ,
    SUPPRESSED,
    REPLAYED
}