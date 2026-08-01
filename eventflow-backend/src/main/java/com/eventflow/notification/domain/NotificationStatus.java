package com.eventflow.notification.domain;

/**
 * Represents the lifecycle status of a notification.
 */
public enum NotificationStatus {
    QUEUED,
    PROCESSING,
    DISPATCHED,
    DELIVERED,
    FAILED,
    DLQ,
    RETRY_SCHEDULED,
    SUPPRESSED;

    public boolean isTerminal() {
        return this == DELIVERED || this == FAILED || this == DLQ || this == SUPPRESSED;
    }

    public boolean canTransitionTo(NotificationStatus target) {
        return switch (this) {
            case QUEUED -> target == PROCESSING || target == FAILED
                || target == DISPATCHED || target == DELIVERED
                || target == DLQ || target == RETRY_SCHEDULED;
            case PROCESSING -> target == DISPATCHED || target == FAILED || target == DLQ
                || target == RETRY_SCHEDULED;
            case DISPATCHED -> target == DELIVERED || target == FAILED;
            case RETRY_SCHEDULED -> target == QUEUED || target == DLQ;
            case DELIVERED, FAILED, DLQ, SUPPRESSED -> false;
        };
    }
}