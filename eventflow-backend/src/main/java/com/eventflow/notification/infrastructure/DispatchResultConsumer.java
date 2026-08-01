package com.eventflow.notification.infrastructure;

import com.eventflow.common.infrastructure.EventFlowProperties;
import com.eventflow.notification.application.NotificationEventPublisher;
import com.eventflow.notification.application.NotificationRepository;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationEvent;
import com.eventflow.notification.domain.NotificationEventType;
import com.eventflow.notification.domain.NotificationStatus;
import com.eventflow.notification.domain.events.DispatchResultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka consumer for processing dispatch.result events.
 * Handles the outcome of provider dispatch operations and manages
 * the notification lifecycle accordingly (success, retry, DLQ).
 */
@Component
public class DispatchResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(DispatchResultConsumer.class);

    private final NotificationRepository notificationRepository;
    private final EventFlowProperties eventFlowProperties;

    public DispatchResultConsumer(NotificationRepository notificationRepository,
                                  EventFlowProperties eventFlowProperties) {
        this.notificationRepository = notificationRepository;
        this.eventFlowProperties = eventFlowProperties;
    }

    @KafkaListener(
        topics = "dispatch.result",
        groupId = "eventflow-processor-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onDispatchResult(DispatchResultEvent event,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 Acknowledgment ack) {
        log.info("Processing dispatch result: eventId={}, notificationId={}, success={}, transientFailure={}",
            event.getEventId(), event.notificationId(), event.success(), event.transientFailure());

        try {
            UUID notificationId = event.notificationId();

            Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalStateException(
                    "Notification not found for dispatch result: " + notificationId));

            if (event.success()) {
                handleSuccess(notification, event);
            } else if (event.transientFailure()) {
                handleTransientFailure(notification, event);
            } else {
                handlePermanentFailure(notification, event);
            }

            notificationRepository.save(notification);
            ack.acknowledge();

            log.info("Dispatch result processed: notificationId={}, newStatus={}",
                notificationId, notification.getStatus());
        } catch (Exception e) {
            log.error("Error processing dispatch result: eventId={}", event.getEventId(), e);
            throw e;
        }
    }

    private void handleSuccess(Notification notification, DispatchResultEvent event) {
        notification.markDelivered();
        log.info("Notification delivered successfully: id={}", notification.getId());
    }

    private void handleTransientFailure(Notification notification, DispatchResultEvent event) {
        int maxRetries = eventFlowProperties.getRetry().getMaxAttempts();

        if (notification.hasExceededMaxRetries(maxRetries)) {
            notification.markDlq(event.errorMessage());
            log.warn("Notification exceeded max retries ({}), moved to DLQ: id={}",
                maxRetries, notification.getId());
        } else {
            // Calculate next retry time with exponential backoff and jitter
            long baseDelay = eventFlowProperties.getRetry().getBaseDelayMs();
            int multiplier = eventFlowProperties.getRetry().getMultiplier();
            double jitter = eventFlowProperties.getRetry().getJitterPercentage();

            long delay = (long) (baseDelay * Math.pow(multiplier, notification.getAttemptCount()));
            // Add jitter: ±jitterPercentage
            long jitterAmount = (long) (delay * jitter);
            delay = delay + (long) ((Math.random() * 2 - 1) * jitterAmount);

            Instant nextRetry = Instant.now().plus(Duration.ofMillis(delay));
            notification.markRetryScheduled(nextRetry);

            log.info("Notification scheduled for retry: id={}, attempt={}, nextRetryAt={}",
                notification.getId(), notification.getAttemptCount(), nextRetry);
        }
    }

    private void handlePermanentFailure(Notification notification, DispatchResultEvent event) {
        notification.markFailed(event.errorMessage());
        log.error("Notification permanently failed: id={}, error={}",
            notification.getId(), event.errorMessage());
    }
}