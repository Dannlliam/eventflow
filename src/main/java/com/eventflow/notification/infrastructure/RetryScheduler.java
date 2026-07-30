package com.eventflow.notification.infrastructure;

import com.eventflow.common.infrastructure.EventFlowProperties;
import com.eventflow.notification.application.NotificationEventPublisher;
import com.eventflow.notification.application.NotificationRepository;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationStatus;
import com.eventflow.notification.domain.events.NotificationCreatedEvent;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled task that picks up RETRY_SCHEDULED notifications whose nextRetryAt
 * timestamp has passed, and re-publishes them to the notification.created topic
 * for reprocessing.
 * <p>
 * Uses Redis distributed locks to ensure only one instance processes retries
 * in a multi-instance deployment.
 */
@Component
public class RetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetryScheduler.class);
    private static final String LOCK_KEY = "eventflow:retry-scheduler:lock";
    private static final int LOCK_TTL_SECONDS = 30;
    private static final int BATCH_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher eventPublisher;
    private final EventFlowProperties eventFlowProperties;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    public RetryScheduler(NotificationRepository notificationRepository,
                          NotificationEventPublisher eventPublisher,
                          EventFlowProperties eventFlowProperties,
                          RedissonClient redissonClient,
                          TransactionTemplate transactionTemplate) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
        this.eventFlowProperties = eventFlowProperties;
        this.redissonClient = redissonClient;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Runs every 15 seconds to check for notifications that are due for retry.
     * Uses Redis distributed lock for coordination across instances.
     */
    @Scheduled(fixedRate = 15_000)
    public void processRetries() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            // Try to acquire the lock with a 5-second wait and 30-second TTL
            if (!lock.tryLock(5, LOCK_TTL_SECONDS, TimeUnit.SECONDS)) {
                log.debug("Retry scheduler lock not acquired (another instance is processing)");
                return;
            }

            Instant now = Instant.now();
            List<Notification> dueNotifications = notificationRepository
                .findByStatusAndNextRetryAtBefore(NotificationStatus.RETRY_SCHEDULED, now, BATCH_SIZE);

            if (dueNotifications.isEmpty()) {
                return;
            }

            log.info("Retry scheduler processing {} notifications due for retry", dueNotifications.size());

            for (Notification notification : dueNotifications) {
                try {
                    transactionTemplate.executeWithoutResult(status -> {
                        // Reload notification within transaction
                        notificationRepository.findById(notification.getId()).ifPresent(n -> {
                            // Reset status to QUEUED and increment attempt count
                            // The notification must be in QUEUED state for the consumer to
                            // process it correctly (ProcessNotificationUseCase validates this)
                            n.resetForReplay();
                            // After reset, re-increment attempt since we want to track total attempts
                            n.incrementAttempt();
                            notificationRepository.save(n);

                            // Re-publish the notification.created event onto the retry topic
                            NotificationCreatedEvent retryEvent = n.toCreatedEvent();
                            eventPublisher.publish("notification.retry", n.getId().toString(), retryEvent);

                            log.info("Re-queued notification for retry: id={}, attempt={}",
                                n.getId(), n.getAttemptCount());
                        });
                    });
                } catch (Exception e) {
                    log.error("Failed to process retry for notification: id={}", notification.getId(), e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry scheduler interrupted", e);
        } catch (Exception e) {
            log.error("Error in retry scheduler", e);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}