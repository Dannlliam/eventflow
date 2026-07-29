package com.eventflow.notification.interfaces;

import com.eventflow.notification.application.NotificationEventRepository;
import com.eventflow.notification.application.NotificationRepository;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GraphQL resolver for notification details with event history.
 * Handles nested resolution of events within a notification query.
 * This resolver specializes in the event history sub-resource.
 *
 * The top-level notification queries (notifications, notification) 
 * are handled by AdminGraphQLController.
 */
@Controller
public class NotificationGraphQLService {

    private static final Logger log = LoggerFactory.getLogger(NotificationGraphQLService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationEventRepository notificationEventRepository;

    public NotificationGraphQLService(NotificationRepository notificationRepository,
                                       NotificationEventRepository notificationEventRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationEventRepository = notificationEventRepository;
    }

    /**
     * Batch resolver for loading notification events.
     * Resolves the 'events' field on any Notification type in the GraphQL schema.
     *
     * @param notificationPayload the parent notification
     * @return list of event payloads
     */
    @SchemaMapping(typeName = "Notification", field = "events")
    public List<NotificationEventPayload> resolveEvents(AdminGraphQLController.NotificationPayload notificationPayload) {
        log.debug("Resolving events for notification: id={}", notificationPayload.id());

        UUID notificationId = UUID.fromString(notificationPayload.id());
        List<NotificationEvent> events = notificationEventRepository
            .findByNotificationId(notificationId);

        return events.stream()
            .map(e -> new NotificationEventPayload(
                e.getId().toString(),
                e.getEventType().name(),
                e.getProviderResponse(),
                e.getErrorMessage(),
                e.getCreatedAt().toString()
            ))
            .toList();
    }

    /**
     * Resolves the recipient details for a notification.
     */
    @SchemaMapping(typeName = "Notification", field = "recipient")
    public AdminGraphQLController.RecipientInfoPayload resolveRecipient(
            AdminGraphQLController.NotificationPayload notificationPayload) {
        log.debug("Resolving recipient for notification: id={}", notificationPayload.id());

        return new AdminGraphQLController.RecipientInfoPayload(
            notificationPayload.recipient().email(),
            notificationPayload.recipient().phone(),
            notificationPayload.recipient().deviceToken(),
            notificationPayload.recipient().webhookUrl()
        );
    }
}