package com.eventflow.notification.domain;

import com.eventflow.common.domain.Channel;
import com.eventflow.common.domain.DomainValidationException;
import com.eventflow.common.domain.BaseEntity;
import com.eventflow.notification.domain.events.NotificationCreatedEvent;
import com.eventflow.notification.domain.events.NotificationStatusChangedEvent;
import java.time.Instant;
import java.util.*;

/**
 * Aggregate Root for the Notification entity.
 * Manages the complete lifecycle of a notification request.
 */
public class Notification extends BaseEntity {

    private final UUID workspaceId;
    private final Channel channel;
    private final Recipient recipient;
    private final String templateSlug;
    private final Map<String, String> payload;
    private final Map<String, String> metadata;
    private NotificationStatus status;
    private UUID providerId;
    private String idempotencyKey;
    private int attemptCount;
    private Instant nextRetryAt;
    private Instant sentAt;
    private Instant deliveredAt;
    private final List<NotificationEvent> events;

    public Notification(UUID workspaceId, Channel channel, Recipient recipient,
                        String templateSlug, Map<String, String> payload,
                        Map<String, String> metadata, String idempotencyKey) {
        super();
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.recipient = Objects.requireNonNull(recipient, "recipient must not be null");
        recipient.validateForChannel(channel);
        this.templateSlug = templateSlug;
        this.payload = payload != null ? Collections.unmodifiableMap(new LinkedHashMap<>(payload)) : Map.of();
        this.metadata = metadata != null ? Collections.unmodifiableMap(new LinkedHashMap<>(metadata)) : Map.of();
        this.status = NotificationStatus.QUEUED;
        this.idempotencyKey = idempotencyKey;
        this.attemptCount = 0;
        this.events = new ArrayList<>();
        addEvent(NotificationEventType.CREATED, null, null);
    }

    // Private constructor for reconstitution from persistence
    private Notification(UUID id, UUID workspaceId, Channel channel, Recipient recipient,
                         String templateSlug, Map<String, String> payload,
                         Map<String, String> metadata, NotificationStatus status,
                         UUID providerId, String idempotencyKey, int attemptCount,
                         Instant nextRetryAt, Instant sentAt, Instant deliveredAt,
                         Instant createdAt, Instant updatedAt, long version) {
        super(id, createdAt, updatedAt, version);
        this.workspaceId = workspaceId;
        this.channel = channel;
        this.recipient = recipient;
        this.templateSlug = templateSlug;
        this.payload = payload;
        this.metadata = metadata;
        this.status = status;
        this.providerId = providerId;
        this.idempotencyKey = idempotencyKey;
        this.attemptCount = attemptCount;
        this.nextRetryAt = nextRetryAt;
        this.sentAt = sentAt;
        this.deliveredAt = deliveredAt;
        this.events = new ArrayList<>();
    }

    public static Notification reconstitute(UUID id, UUID workspaceId, Channel channel,
                                             Recipient recipient, String templateSlug,
                                             Map<String, String> payload, Map<String, String> metadata,
                                             NotificationStatus status, UUID providerId,
                                             String idempotencyKey, int attemptCount,
                                             Instant nextRetryAt, Instant sentAt, Instant deliveredAt,
                                             Instant createdAt, Instant updatedAt, long version) {
        return new Notification(id, workspaceId, channel, recipient, templateSlug,
            payload, metadata, status, providerId, idempotencyKey, attemptCount,
            nextRetryAt, sentAt, deliveredAt, createdAt, updatedAt, version);
    }

    public void markProcessing() {
        transitionTo(NotificationStatus.PROCESSING);
        addEvent(NotificationEventType.PROCESSING, null, null);
    }

    public void markDispatched(UUID providerId) {
        this.providerId = providerId;
        this.sentAt = Instant.now();
        transitionTo(NotificationStatus.DISPATCHED);
        addEvent(NotificationEventType.DISPATCHED, null, null);
    }

    public void markDelivered() {
        this.deliveredAt = Instant.now();
        transitionTo(NotificationStatus.DELIVERED);
        addEvent(NotificationEventType.DELIVERED, null, null);
    }

    public void markFailed(String errorMessage) {
        transitionTo(NotificationStatus.FAILED);
        addEvent(NotificationEventType.FAILED, null, errorMessage);
    }

    public void markRetryScheduled(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
        this.attemptCount++;
        transitionTo(NotificationStatus.RETRY_SCHEDULED);
        addEvent(NotificationEventType.RETRY_SCHEDULED, null,
            "Retry scheduled at " + nextRetryAt + " (attempt " + attemptCount + ")");
    }

    public void markDlq(String errorMessage) {
        transitionTo(NotificationStatus.DLQ);
        addEvent(NotificationEventType.DLQ, null, errorMessage);
    }

    public void markSuppressed() {
        transitionTo(NotificationStatus.SUPPRESSED);
        addEvent(NotificationEventType.SUPPRESSED, null, "Message suppressed");
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }

    /**
     * Resets the notification for DLQ replay.
     * Sets status back to QUEUED, resets attempt count,
     * clears provider ID and failure timestamps.
     */
    public void resetForReplay() {
        this.status = NotificationStatus.QUEUED;
        this.attemptCount = 0;
        this.providerId = null;
        this.nextRetryAt = null;
        this.sentAt = null;
        this.deliveredAt = null;
        markUpdated();
    }

    public boolean hasExceededMaxRetries(int maxRetries) {
        return attemptCount >= maxRetries;
    }

    public boolean isIdempotent() {
        return idempotencyKey != null && !idempotencyKey.isBlank();
    }

    private void transitionTo(NotificationStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new DomainValidationException(
                "INVALID_STATUS_TRANSITION",
                "Cannot transition from " + status + " to " + target
            );
        }
        this.status = target;
        markUpdated();
    }

    private void addEvent(NotificationEventType eventType, String providerResponse, String errorMessage) {
        this.events.add(new NotificationEvent(getId(), eventType, providerResponse, errorMessage));
    }

    public NotificationCreatedEvent toCreatedEvent() {
        return new NotificationCreatedEvent(
            getId(), workspaceId, channel, recipient, templateSlug, payload, metadata
        );
    }

    public NotificationStatusChangedEvent toStatusChangedEvent(NotificationStatus previousStatus) {
        return new NotificationStatusChangedEvent(getId(), workspaceId, previousStatus, status);
    }

    // Getters
    public UUID getWorkspaceId() { return workspaceId; }
    public Channel getChannel() { return channel; }
    public Recipient getRecipient() { return recipient; }
    public Optional<String> getTemplateSlug() { return Optional.ofNullable(templateSlug); }
    public Map<String, String> getPayload() { return payload; }
    public Map<String, String> getMetadata() { return metadata; }
    public NotificationStatus getStatus() { return status; }
    public Optional<UUID> getProviderId() { return Optional.ofNullable(providerId); }
    public Optional<String> getIdempotencyKey() { return Optional.ofNullable(idempotencyKey); }
    public int getAttemptCount() { return attemptCount; }
    public Optional<Instant> getNextRetryAt() { return Optional.ofNullable(nextRetryAt); }
    public Optional<Instant> getSentAt() { return Optional.ofNullable(sentAt); }
    public Optional<Instant> getDeliveredAt() { return Optional.ofNullable(deliveredAt); }
    public List<NotificationEvent> getEvents() { return Collections.unmodifiableList(events); }
}