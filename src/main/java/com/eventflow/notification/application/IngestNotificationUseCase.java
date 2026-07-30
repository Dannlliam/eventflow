package com.eventflow.notification.application;

import com.eventflow.common.domain.Channel;
import com.eventflow.common.domain.DomainValidationException;
import com.eventflow.common.infrastructure.ObservabilityConfig;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.Recipient;
import com.eventflow.notification.domain.events.NotificationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case for ingesting a notification request.
 * Validates the payload, checks idempotency, persists the notification,
 * and emits a domain event for further processing.
 * Records observability metrics for monitoring.
 */
public class IngestNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(IngestNotificationUseCase.class);

    private final NotificationRepository notificationRepository;
    private final NotificationEventRepository eventRepository;
    private final NotificationEventPublisher eventPublisher;
    private final ObservabilityConfig.NotificationMetrics notificationMetrics;

    public IngestNotificationUseCase(NotificationRepository notificationRepository,
                                     NotificationEventRepository eventRepository,
                                     NotificationEventPublisher eventPublisher,
                                     ObservabilityConfig.NotificationMetrics notificationMetrics) {
        this.notificationRepository = notificationRepository;
        this.eventRepository = eventRepository;
        this.eventPublisher = eventPublisher;
        this.notificationMetrics = notificationMetrics;
    }

    public IngestResult execute(IngestCommand command) {
        // Validate channel
        Channel channel = Channel.fromString(command.channel());

        // Validate and create recipient
        Recipient recipient = new Recipient(
            command.recipientEmail(),
            command.recipientPhone(),
            command.recipientDeviceToken(),
            command.recipientWebhookUrl()
        );

        // Check idempotency
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            Optional<Notification> existing = notificationRepository.findByIdempotencyKey(
                command.workspaceId(), command.idempotencyKey()
            );
            if (existing.isPresent()) {
                log.info("Idempotent request detected: workspaceId={}, idempotencyKey={}, existingId={}",
                    command.workspaceId(), command.idempotencyKey(), existing.get().getId());
                return new IngestResult(existing.get().getId(), existing.get().getStatus().name(), true);
            }
        }

        // Create and persist notification
        Notification notification = new Notification(
            command.workspaceId(),
            channel,
            recipient,
            command.templateSlug(),
            command.payload(),
            command.metadata(),
            command.idempotencyKey()
        );

        Notification saved = notificationRepository.save(notification);

        // Persist initial events
        notification.getEvents().forEach(eventRepository::save);

        // Emit domain event for Kafka processing
        NotificationCreatedEvent domainEvent = notification.toCreatedEvent();
        eventPublisher.publishNotificationCreated(domainEvent);

        // Record observability metrics
        notificationMetrics.recordIngestion();
        notificationMetrics.recordPayloadSize(command.payload() != null
            ? command.payload().toString().length() : 0);

        log.info("Notification ingested and published: id={}, workspaceId={}, channel={}, eventId={}",
            saved.getId(), saved.getWorkspaceId(), saved.getChannel(), domainEvent.getEventId());

        return new IngestResult(saved.getId(), saved.getStatus().name(), false);
    }

    public record IngestCommand(
        UUID workspaceId,
        String channel,
        String recipientEmail,
        String recipientPhone,
        String recipientDeviceToken,
        String recipientWebhookUrl,
        String templateSlug,
        Map<String, String> payload,
        Map<String, String> metadata,
        String idempotencyKey
    ) {}

    public record IngestResult(UUID eventId, String status, boolean deduplicated) {}
}