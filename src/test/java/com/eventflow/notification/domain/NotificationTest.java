package com.eventflow.notification.domain;

import com.eventflow.common.domain.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;

/**
 * Unit tests for the Notification aggregate root.
 */
@DisplayName("Notification aggregate root")
class NotificationTest {

    private final UUID workspaceId = UUID.randomUUID();
    private final Recipient recipient = new Recipient("user@example.com", null, null, null);
    private final Map<String, String> payload = Map.of("name", "John");

    @Test
    @DisplayName("should create notification with QUEUED status")
    void constructor_ValidInputs_SetsQueuedStatus() {
        Notification notification = new Notification(
            workspaceId, Channel.EMAIL, recipient, "welcome-email", payload, null, null);

        assertNotNull(notification.getId());
        assertEquals(workspaceId, notification.getWorkspaceId());
        assertEquals(Channel.EMAIL, notification.getChannel());
        assertEquals(recipient, notification.getRecipient());
        assertEquals(NotificationStatus.QUEUED, notification.getStatus());
        assertEquals("welcome-email", notification.getTemplateSlug().orElse(null));
        assertEquals(0, notification.getAttemptCount());
        assertTrue(notification.getNextRetryAt().isEmpty());
        assertTrue(notification.getProviderId().isEmpty());
        assertTrue(notification.getIdempotencyKey().isEmpty());
    }

    @Test
    @DisplayName("should create notification with idempotency key when provided")
    void constructor_WithIdempotencyKey_SetsKey() {
        String idempotencyKey = "idem-123";
        Notification notification = new Notification(
            workspaceId, Channel.SMS,
            new Recipient(null, "+1234567890", null, null),
            null, payload, null, idempotencyKey);

        assertEquals(idempotencyKey, notification.getIdempotencyKey().orElse(null));
    }

    @Test
    @DisplayName("should transition to PROCESSING status")
    void markProcessing_UpdatesStatus() {
        Notification notification = createBasicNotification();
        notification.markProcessing();
        assertEquals(NotificationStatus.PROCESSING, notification.getStatus());
    }

    @Test
    @DisplayName("should transition to DISPATCHED status with provider ID")
    void markDispatched_UpdatesStatus() {
        Notification notification = createBasicNotification();
        UUID providerId = UUID.randomUUID();

        notification.markDispatched(providerId);

        assertEquals(NotificationStatus.DISPATCHED, notification.getStatus());
        assertEquals(providerId, notification.getProviderId().orElse(null));
        assertNotNull(notification.getSentAt().orElse(null));
    }

    @Test
    @DisplayName("should transition to DELIVERED status")
    void markDelivered_UpdatesStatus() {
        Notification notification = createBasicNotification();
        notification.markDelivered();
        assertEquals(NotificationStatus.DELIVERED, notification.getStatus());
        assertNotNull(notification.getDeliveredAt().orElse(null));
    }

    @Test
    @DisplayName("should transition to FAILED status")
    void markFailed_UpdatesStatus() {
        Notification notification = createBasicNotification();
        notification.markFailed("Connection timeout");
        assertEquals(NotificationStatus.FAILED, notification.getStatus());
    }

    @Test
    @DisplayName("should transition to DLQ status")
    void markDlq_UpdatesStatus() {
        Notification notification = createBasicNotification();
        notification.markDlq("Max retries exceeded");
        assertEquals(NotificationStatus.DLQ, notification.getStatus());
    }

    @Test
    @DisplayName("should increment attempt count")
    void incrementAttempt_IncreasesCount() {
        Notification notification = createBasicNotification();
        assertEquals(0, notification.getAttemptCount());
        notification.incrementAttempt();
        assertEquals(1, notification.getAttemptCount());
        notification.incrementAttempt();
        assertEquals(2, notification.getAttemptCount());
    }

    @Test
    @DisplayName("should schedule retry with next retry timestamp")
    void scheduleRetry_SetsNextRetryAt() {
        Notification notification = createBasicNotification();
        java.time.Instant nextRetry = java.time.Instant.now().plusSeconds(60);

        notification.markRetryScheduled(nextRetry);

        assertEquals(NotificationStatus.RETRY_SCHEDULED, notification.getStatus());
        assertTrue(notification.getNextRetryAt().isPresent());
        assertEquals(nextRetry, notification.getNextRetryAt().get());
    }

    private Notification createBasicNotification() {
        return new Notification(workspaceId, Channel.EMAIL, recipient, null, payload, null, null);
    }
}