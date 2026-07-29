package com.eventflow.notification.infrastructure;

import com.eventflow.common.domain.Channel;
import com.eventflow.common.infrastructure.EventFlowProperties;
import com.eventflow.notification.application.NotificationEventPublisher;
import com.eventflow.notification.application.NotificationRepository;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationStatus;
import com.eventflow.notification.domain.events.DispatchRequestedEvent;
import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.eventflow.notification.domain.events.NotificationCreatedEvent;
import com.eventflow.provider.application.ProviderRepository;
import com.eventflow.provider.domain.Provider;
import com.eventflow.template.application.TemplateRendererPort;
import com.eventflow.template.application.TemplateRepository;
import com.eventflow.template.domain.RenderedContent;
import com.eventflow.template.domain.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

/**
 * Kafka consumer for processing notification.created events.
 * This is the core processor that handles the notification lifecycle:
 * 1. Fetch notification from database
 * 2. Mark as PROCESSING
 * 3. Look up template and render content
 * 4. Look up provider configuration
 * 5. Publish dispatch.requested event to Kafka
 * 6. Handle failures with retry logic
 */
@Component
public class NotificationCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationCreatedConsumer.class);

    private final NotificationRepository notificationRepository;
    private final TemplateRepository templateRepository;
    private final TemplateRendererPort templateRenderer;
    private final ProviderRepository providerRepository;
    private final NotificationEventPublisher eventPublisher;
    private final EventFlowProperties eventFlowProperties;
    private final TransactionTemplate transactionTemplate;

    public NotificationCreatedConsumer(NotificationRepository notificationRepository,
                                       TemplateRepository templateRepository,
                                       TemplateRendererPort templateRenderer,
                                       ProviderRepository providerRepository,
                                       NotificationEventPublisher eventPublisher,
                                       EventFlowProperties eventFlowProperties,
                                       TransactionTemplate transactionTemplate) {
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
        this.templateRenderer = templateRenderer;
        this.providerRepository = providerRepository;
        this.eventPublisher = eventPublisher;
        this.eventFlowProperties = eventFlowProperties;
        this.transactionTemplate = transactionTemplate;
    }

    @KafkaListener(
        topics = "notification.created",
        groupId = "eventflow-processor-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onNotificationCreated(NotificationCreatedEvent event,
                                      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                      @Header(KafkaHeaders.OFFSET) long offset,
                                      Acknowledgment ack) {
        log.info("Processing notification created event: eventId={}, notificationId={}, channel={}",
            event.eventId(), event.notificationId(), event.channel());

        try {
            processNotification(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing notification created event: eventId={}", event.getEventId(), e);
            // Error handler will manage retries and DLQ
            throw e;
        }
    }

    @KafkaListener(
        topics = "notification.retry",
        groupId = "eventflow-processor-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onNotificationRetry(NotificationCreatedEvent event,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                    @Header(KafkaHeaders.OFFSET) long offset,
                                    Acknowledgment ack) {
        log.info("Processing retry for notification: eventId={}, notificationId={}",
            event.getEventId(), event.notificationId());
        onNotificationCreated(event, partition, offset, ack);
    }

    /**
     * Core notification processing logic.
     * Executed within a transaction to ensure consistency.
     */
    private void processNotification(NotificationCreatedEvent event) {
        UUID notificationId = event.notificationId();

        // 1. Fetch notification from database
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalStateException(
                "Notification not found: " + notificationId));

        log.debug("Fetched notification: id={}, status={}, attempt={}",
            notificationId, notification.getStatus(), notification.getAttemptCount());

        // 2. Mark as PROCESSING
        notification.markProcessing();
        notificationRepository.save(notification);

        // 3. Render template (if template slug is provided)
        String subject = "";
        String body = "";
        String recipientAddress = resolveRecipientAddress(notification);

        Optional<String> templateSlug = notification.getTemplateSlug();
        if (templateSlug.isPresent() && !templateSlug.get().isBlank()) {
            Optional<Template> template = templateRepository.findBySlug(templateSlug.get());
            if (template.isPresent() && template.get().getActiveVersion().isPresent()) {
                var activeVersion = template.get().getActiveVersion().get();
                RenderedContent rendered = templateRenderer.render(
                    activeVersion.getBodyTemplate(),
                    activeVersion.getSubjectTemplate(),
                    notification.getPayload()
                );
                subject = rendered.subject();
                body = rendered.htmlBody();
                log.debug("Rendered template: slug={}, subject={}", templateSlug.get(), subject);
            } else {
                log.warn("Template not found or no active version: slug={}", templateSlug.get());
            }
        } else {
            // No template - use payload as body
            body = notification.getPayload().toString();
        }

        // 4. Look up provider configuration
        Provider provider = providerRepository.findPrimaryByWorkspaceIdAndChannel(
                notification.getWorkspaceId(), notification.getChannel())
            .orElseThrow(() -> new IllegalStateException(
                "No active provider found for workspace " + notification.getWorkspaceId()
                    + " and channel " + notification.getChannel()));

        log.debug("Resolved provider: id={}, type={}, name={}",
            provider.getId(), provider.getProviderType(), provider.getName());

        // 5. Publish dispatch.requested event
        DispatchRequestedEvent dispatchEvent = new DispatchRequestedEvent(
            notificationId,
            notification.getWorkspaceId(),
            notification.getChannel().name(),
            provider.getProviderType().name(),
            recipientAddress,
            subject,
            body
        );

        eventPublisher.publish("dispatch.requested", notificationId.toString(), dispatchEvent);

        log.info("Published dispatch.requested event: notificationId={}, providerType={}, recipient={}",
            notificationId, provider.getProviderType(), recipientAddress);
    }

    /**
     * Resolves the recipient address string based on the notification channel.
     */
    private String resolveRecipientAddress(Notification notification) {
        var recipient = notification.getRecipient();
        return switch (notification.getChannel()) {
            case EMAIL -> recipient.email();
            case SMS -> recipient.phone();
            case PUSH -> recipient.deviceToken();
            case WEBHOOK -> recipient.webhookUrl();
        };
    }
}