package com.eventflow.notification.application;

import com.eventflow.common.domain.Channel;
import com.eventflow.common.domain.DomainValidationException;
import com.eventflow.common.infrastructure.PhoneNumberNormalizationService;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationStatus;
import com.eventflow.notification.domain.Recipient;
import com.eventflow.notification.domain.events.DispatchRequestedEvent;
import com.eventflow.notification.domain.events.NotificationCreatedEvent;
import com.eventflow.provider.application.ProviderRepository;
import com.eventflow.provider.domain.Provider;
import com.eventflow.template.application.TemplateRendererPort;
import com.eventflow.template.application.TemplateRepository;
import com.eventflow.template.domain.RenderedContent;
import com.eventflow.template.domain.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Use case for processing a notification from ingestion through to dispatch.
 * Handles:
 * 1. Notification state machine transitions (QUEUED -> PROCESSING)
 * 2. Template rendering (if template slug provided)
 * 3. Provider resolution (primary/secondary failover)
 * 4. Publishing dispatch.requested event
 *
 * As specified in the PRD Sections 29, 49, 54.
 */
public class ProcessNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessNotificationUseCase.class);

    private final NotificationRepository notificationRepository;
    private final NotificationEventRepository eventRepository;
    private final NotificationEventPublisher eventPublisher;
    private final TemplateRepository templateRepository;
    private final TemplateRendererPort templateRenderer;
    private final ProviderRepository providerRepository;
    private final PhoneNumberNormalizationService phoneNumberNormalization;

    public ProcessNotificationUseCase(NotificationRepository notificationRepository,
                                      NotificationEventRepository eventRepository,
                                      NotificationEventPublisher eventPublisher,
                                      TemplateRepository templateRepository,
                                      TemplateRendererPort templateRenderer,
                                      ProviderRepository providerRepository,
                                      PhoneNumberNormalizationService phoneNumberNormalization) {
        this.notificationRepository = notificationRepository;
        this.eventRepository = eventRepository;
        this.eventPublisher = eventPublisher;
        this.templateRepository = templateRepository;
        this.templateRenderer = templateRenderer;
        this.providerRepository = providerRepository;
        this.phoneNumberNormalization = phoneNumberNormalization;
    }

    /**
     * Processes a notification from its current state.
     *
     * @param notificationId the notification UUID
     * @param event the original notification created event
     * @return ProcessingResult containing the result of the processing
     */
    public ProcessingResult execute(UUID notificationId, NotificationCreatedEvent event) {
        log.info("Processing notification: id={}, channel={}", notificationId, event.channel());

        // 1. Fetch notification from database
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalStateException(
                "Notification not found: " + notificationId));

        // 2. Validate state: must be QUEUED or RETRY_SCHEDULED
        NotificationStatus currentStatus = notification.getStatus();
        if (currentStatus != NotificationStatus.QUEUED &&
            currentStatus != NotificationStatus.RETRY_SCHEDULED) {
            log.warn("Notification not in valid state for processing: id={}, status={}",
                notificationId, currentStatus);
            return new ProcessingResult(notificationId, currentStatus, false, "Invalid state: " + currentStatus);
        }

        // 3. Mark as PROCESSING
        notification.markProcessing();
        notificationRepository.save(notification);

        // 4. Resolve recipient address with channel-specific validation
        String recipientAddress = resolveAndNormalizeRecipient(notification);
        if (recipientAddress == null || recipientAddress.isBlank()) {
            String error = "Invalid recipient address for channel: " + notification.getChannel();
            log.warn("{}: id={}", error, notificationId);
            notification.markFailed(error);
            notificationRepository.save(notification);
            return new ProcessingResult(notificationId, notification.getStatus(), false, error);
        }

        // 5. Render template (if template slug is provided)
        String subject = "";
        String body = "";
        Optional<String> templateSlug = notification.getTemplateSlug();
        if (templateSlug.isPresent() && !templateSlug.get().isBlank()) {
            try {
                Optional<Template> template = templateRepository.findBySlug(templateSlug.get());
                if (template.isPresent() && template.get().getActiveVersion().isPresent()) {
                    var activeVersion = template.get().getActiveVersion().get();
                    RenderedContent rendered = templateRenderer.render(
                        activeVersion.getBodyTemplate(),
                        activeVersion.getSubjectTemplate(),
                        notification.getPayload()
                    );
                    subject = rendered.subject() != null ? rendered.subject() : "";
                    body = rendered.htmlBody() != null ? rendered.htmlBody() : rendered.textBody();
                    log.debug("Rendered template: slug={}, subject={}", templateSlug.get(), subject);
                } else {
                    log.warn("Template not found or no active version: slug={}", templateSlug.get());
                    body = notification.getPayload().toString();
                }
            } catch (Exception e) {
                log.error("Template rendering failed: slug={}, error={}", templateSlug.get(), e.getMessage());
                body = notification.getPayload().toString();
            }
        } else {
            body = notification.getPayload().toString();
        }

        // 6. Resolve provider (primary, with fallback support)
        Provider provider = resolveProvider(notification.getWorkspaceId(), notification.getChannel());
        if (provider == null) {
            String error = "No active provider found for workspace " + notification.getWorkspaceId()
                + " and channel " + notification.getChannel();
            log.error("{}", error);
            notification.markFailed(error);
            notificationRepository.save(notification);
            return new ProcessingResult(notificationId, notification.getStatus(), false, error);
        }

        // 7. Mark as DISPATCHED and publish event
        notification.markDispatched(provider.getId());
        notificationRepository.save(notification);

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

        return new ProcessingResult(notificationId, notification.getStatus(), true, null);
    }

    /**
     * Resolves and normalizes the recipient address based on channel.
     * For SMS, normalizes phone numbers to E.164 format.
     */
    private String resolveAndNormalizeRecipient(Notification notification) {
        Recipient recipient = notification.getRecipient();
        return switch (notification.getChannel()) {
            case EMAIL -> recipient.email();
            case SMS -> {
                String phone = recipient.phone();
                if (phone != null && !phone.isBlank()) {
                    yield phoneNumberNormalization.normalizeToE164(phone)
                        .orElse(null);
                }
                yield null;
            }
            case PUSH -> recipient.deviceToken();
            case WEBHOOK -> recipient.webhookUrl();
        };
    }

    /**
     * Resolves the provider for a workspace and channel.
     * Tries primary provider first, then falls back to secondary.
     */
    private Provider resolveProvider(UUID workspaceId, Channel channel) {
        // Try primary provider first
        Optional<Provider> primary = providerRepository.findPrimaryByWorkspaceIdAndChannel(workspaceId, channel);
        if (primary.isPresent() && primary.get().isEnabled()) {
            log.debug("Using primary provider: id={}, type={}", primary.get().getId(), primary.get().getProviderType());
            return primary.get();
        }

        // Fallback to any enabled provider for this channel
        Optional<Provider> fallback = providerRepository.findFirstByWorkspaceIdAndChannelAndEnabled(workspaceId, channel);
        if (fallback.isPresent()) {
            log.info("Primary provider unavailable, using fallback: id={}, type={}",
                fallback.get().getId(), fallback.get().getProviderType());
            return fallback.get();
        }

        return null;
    }

    /**
     * Result of processing a notification.
     */
    public record ProcessingResult(
        UUID notificationId,
        NotificationStatus status,
        boolean success,
        String errorMessage
    ) {}
}