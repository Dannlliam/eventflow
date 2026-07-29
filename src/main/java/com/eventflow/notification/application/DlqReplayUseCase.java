package com.eventflow.notification.application;

import com.eventflow.common.domain.DomainValidationException;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationStatus;
import com.eventflow.notification.domain.events.NotificationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Use case for replaying a message from the Dead Letter Queue (DLQ).
 * When an SRE clicks the "Replay DLQ" button in the Admin Dashboard:
 * 1. Fetches the DLQ message from database
 * 2. Resets attempt_count to 0
 * 3. Sets status back to QUEUED
 * 4. Re-publishes the notification.created event to Kafka
 * 5. Generates an audit log entry
 *
 * As specified in the PRD Section 30 - Dead Letter Queue / Replay Mechanism.
 */
public class DlqReplayUseCase {

    private static final Logger log = LoggerFactory.getLogger(DlqReplayUseCase.class);

    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher eventPublisher;

    public DlqReplayUseCase(NotificationRepository notificationRepository,
                            NotificationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Replays a DLQ message back to the processing queue.
     *
     * @param notificationId the UUID of the notification in DLQ
     * @param replayedBy the user ID of the SRE performing the replay
     * @return ReplayResult containing the result of the operation
     * @throws DomainValidationException if the notification is not in DLQ status
     * @throws IllegalStateException if the notification is not found
     */
    public ReplayResult execute(UUID notificationId, UUID replayedBy) {
        log.info("DLQ replay requested: notificationId={}, replayedBy={}", notificationId, replayedBy);

        // 1. Fetch the notification
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalStateException(
                "Notification not found for DLQ replay: " + notificationId));

        // 2. Validate it's actually in DLQ state
        if (notification.getStatus() != NotificationStatus.DLQ) {
            throw new DomainValidationException(
                "INVALID_DLQ_REPLAY",
                "Notification " + notificationId + " is not in DLQ status. Current status: " + notification.getStatus()
            );
        }

        // 3. Reset the notification to QUEUED state for reprocessing
        notification.resetForReplay();
        notificationRepository.save(notification);

        // 4. Get the event details for re-publishing
        NotificationCreatedEvent originalEvent = new NotificationCreatedEvent(
            notification.getId(),
            notification.getWorkspaceId(),
            notification.getChannel(),
            notification.getRecipient(),
            notification.getTemplateSlug().orElse(null),
            notification.getPayload(),
            notification.getMetadata()
        );

        // 5. Publish the notification.created event for reprocessing
        eventPublisher.publishNotificationCreated(originalEvent);

        log.info("DLQ message replayed successfully: notificationId={}, originalEventId={}, replayedBy={}",
            notificationId, originalEvent.getEventId(), replayedBy);

        return new ReplayResult(notificationId, true, "DLQ message replayed successfully");
    }

    /**
     * Replays multiple DLQ messages in a batch.
     *
     * @param notificationIds list of notification UUIDs to replay
     * @param replayedBy the user ID of the SRE performing the replay
     * @return BatchReplayResult with counts of succeeded and failed replays
     */
    public BatchReplayResult executeBatch(java.util.List<UUID> notificationIds, UUID replayedBy) {
        int succeeded = 0;
        int failed = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();

        for (UUID notificationId : notificationIds) {
            try {
                ReplayResult result = execute(notificationId, replayedBy);
                if (result.success()) {
                    succeeded++;
                } else {
                    failed++;
                    errors.add(result.message());
                }
            } catch (Exception e) {
                failed++;
                errors.add("Notification " + notificationId + ": " + e.getMessage());
                log.error("Failed to replay DLQ notification: id={}, error={}", notificationId, e.getMessage());
            }
        }

        log.info("Batch DLQ replay completed: succeeded={}, failed={}, total={}",
            succeeded, failed, notificationIds.size());

        return new BatchReplayResult(succeeded, failed, errors);
    }

    /**
     * Result of a single DLQ replay operation.
     */
    public record ReplayResult(
        UUID notificationId,
        boolean success,
        String message
    ) {}

    /**
     * Result of a batch DLQ replay operation.
     */
    public record BatchReplayResult(
        int succeeded,
        int failed,
        java.util.List<String> errors
    ) {}
}