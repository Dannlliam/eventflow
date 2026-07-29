package com.eventflow.notification.application;

import com.eventflow.common.infrastructure.EventFlowProperties;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationStatus;
import com.eventflow.notification.domain.events.DispatchResultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case for handling dispatch results from providers.
 * Processes dispatch.result events and updates the notification state machine:
 * - Success: transition to DELIVERED
 * - Transient failure: schedule retry with exponential backoff
 * - Permanent failure: route to DLQ
 *
 * As specified in the PRD Sections 29-30, 56.
 */
public class HandleDispatchResultUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleDispatchResultUseCase.class);

    private final NotificationRepository notificationRepository;
    private final NotificationEventRepository eventRepository;
    private final NotificationEventPublisher eventPublisher;
    private final EventFlowProperties eventFlowProperties;

    public HandleDispatchResultUseCase(NotificationRepository notificationRepository,
                                       NotificationEventRepository eventRepository,
                                       NotificationEventPublisher eventPublisher,
                                       EventFlowProperties eventFlowProperties) {
        this.notificationRepository = notificationRepository;
        this.eventRepository = eventRepository;
        this.eventPublisher = eventPublisher;
        this.eventFlowProperties = eventFlowProperties;
    }

    /**
     * Handles a dispatch result event.
     *
     * @param event the dispatch result from the provider
     * @return DispatchHandlingResult indicating the outcome
     */
    public DispatchHandlingResult execute(DispatchResultEvent event) {
        UUID notificationId = event.notificationId();

        log.info("Handling dispatch result: notificationId={}, success={}, transient={}, httpStatus={}",
            notificationId, event.success(), event.transientFailure(), event.httpStatusCode());

        // Fetch notification
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalStateException(
                "Notification not found for dispatch result: " + notificationId));

        // Validate current state
        NotificationStatus currentStatus = notification.getStatus();
        if (currentStatus != NotificationStatus.DISPATCHED &&
            currentStatus != NotificationStatus.PROCESSING) {
            log.warn("Notification in unexpected state for dispatch result: id={}, status={}",
                notificationId, currentStatus);
            return new DispatchHandlingResult(notificationId, currentStatus, false,
                "Unexpected state: " + currentStatus);
        }

        if (event.success()) {
            return handleSuccess(notification, event);
        } else if (event.transientFailure()) {
            return handleTransientFailure(notification, event);
        } else {
            return handlePermanentFailure(notification, event);
        }
    }

    /**
     * Handles a successful dispatch.
     * Transitions the notification to DELIVERED state.
     */
    private DispatchHandlingResult handleSuccess(Notification notification, DispatchResultEvent event) {
        notification.markDelivered();
        notificationRepository.save(notification);

        log.info("Notification delivered successfully: id={}, provider={}",
            notification.getId(), event.providerType());

        return new DispatchHandlingResult(
            notification.getId(),
            NotificationStatus.DELIVERED,
            true,
            null
        );
    }

    /**
     * Handles a transient failure.
     * Calculates exponential backoff and schedules a retry.
     * If max retries exceeded, routes to DLQ.
     */
    private DispatchHandlingResult handleTransientFailure(Notification notification, DispatchResultEvent event) {
        int maxRetries = eventFlowProperties.getRetry().getMaxAttempts();
        int currentAttempt = notification.getAttemptCount();

        if (notification.hasExceededMaxRetries(maxRetries)) {
            return routeToDlq(notification, event,
                "Max retries exceeded: " + currentAttempt + "/" + maxRetries);
        }

        // Calculate exponential backoff with jitter
        Instant nextRetryAt = calculateNextRetry(currentAttempt);
        notification.markRetryScheduled(nextRetryAt);
        notificationRepository.save(notification);

        log.info("Retry scheduled for notification: id={}, attempt={}/{}, nextRetryAt={}",
            notification.getId(), currentAttempt + 1, maxRetries, nextRetryAt);

        return new DispatchHandlingResult(
            notification.getId(),
            NotificationStatus.RETRY_SCHEDULED,
            true,
            "Retry scheduled at " + nextRetryAt + " (attempt " + (currentAttempt + 1) + ")"
        );
    }

    /**
     * Handles a permanent failure.
     * Routes the notification directly to the DLQ.
     */
    private DispatchHandlingResult handlePermanentFailure(Notification notification, DispatchResultEvent event) {
        return routeToDlq(notification, event,
            "Permanent failure: HTTP " + event.httpStatusCode() + " - " + event.errorMessage());
    }

    /**
     * Routes a notification to the Dead Letter Queue.
     */
    private DispatchHandlingResult routeToDlq(Notification notification, DispatchResultEvent event, String reason) {
        notification.markDlq(reason);
        notificationRepository.save(notification);

        log.warn("Notification routed to DLQ: id={}, reason={}, provider={}",
            notification.getId(), reason, event.providerType());

        return new DispatchHandlingResult(
            notification.getId(),
            NotificationStatus.DLQ,
            false,
            reason
        );
    }

    /**
     * Calculates the next retry timestamp using exponential backoff with jitter.
     * Formula: delay = (baseDelay * 2^attemptNumber) + randomJitter(0-20% of delay)
     *
     * As specified in the PRD Section 29 - Retry Policies / Exponential Backoff Formula.
     */
    private Instant calculateNextRetry(int attemptCount) {
        long baseDelayMs = eventFlowProperties.getRetry().getBaseDelayMs();
        int multiplier = eventFlowProperties.getRetry().getMultiplier();
        double jitterPercentage = eventFlowProperties.getRetry().getJitterPercentage();

        // Calculate exponential delay: baseDelay * 2^attemptCount
        long delayMs = (long) (baseDelayMs * Math.pow(multiplier, attemptCount));

        // Add jitter: random percentage of the delay (0-20%)
        double jitter = delayMs * jitterPercentage * Math.random();
        long totalDelayMs = delayMs + (long) jitter;

        return Instant.now().plusMillis(totalDelayMs);
    }

    /**
     * Result of handling a dispatch result.
     */
    public record DispatchHandlingResult(
        UUID notificationId,
        NotificationStatus status,
        boolean success,
        String message
    ) {}
}