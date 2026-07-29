package com.eventflow.notification.application;

import com.eventflow.common.domain.DomainEvent;
import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.eventflow.notification.domain.events.NotificationCreatedEvent;

/**
 * Port for publishing domain events to Kafka.
 * Abstracts the messaging infrastructure from the application layer.
 */
public interface NotificationEventPublisher {

    /**
     * Publishes a notification.created event.
     */
    void publishNotificationCreated(NotificationCreatedEvent event);

    /**
     * Publishes a dispatch.result event for processing.
     */
    void publishDispatchResult(DispatchResultEvent event);

    /**
     * Publishes a generic domain event.
     */
    void publish(String topic, String key, DomainEvent event);
}