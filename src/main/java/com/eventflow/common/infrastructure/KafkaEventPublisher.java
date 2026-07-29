package com.eventflow.common.infrastructure;

import com.eventflow.common.domain.DomainEvent;
import com.eventflow.notification.application.NotificationEventPublisher;
import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.eventflow.notification.domain.events.NotificationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka implementation of the NotificationEventPublisher port.
 * Publishes domain events to the appropriate Kafka topics with proper headers.
 */
@Component
public class KafkaEventPublisher implements NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private static final String TOPIC_NOTIFICATION_CREATED = "notification.created";
    private static final String TOPIC_DISPATCH_RESULT = "dispatch.result";
    private static final String TOPIC_NOTIFICATION_STATUS = "notification.status.updated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishNotificationCreated(NotificationCreatedEvent event) {
        String key = event.notificationId().toString();
        log.info("Publishing notification.created event: eventId={}, notificationId={}, channel={}",
            event.getEventId(), event.notificationId(), event.channel());
        publish(TOPIC_NOTIFICATION_CREATED, key, event);
    }

    @Override
    public void publishDispatchResult(DispatchResultEvent event) {
        String key = event.notificationId().toString();
        log.info("Publishing dispatch.result event: eventId={}, notificationId={}, success={}",
            event.getEventId(), event.notificationId(), event.success());
        publish(TOPIC_DISPATCH_RESULT, key, event);
    }

    @Override
    public void publish(String topic, String key, DomainEvent event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic {}: key={}, eventId={}, error={}",
                    topic, key, event.getEventId(), ex.getMessage(), ex);
            } else {
                log.debug("Successfully published event to topic {}: key={}, eventId={}, partition={}, offset={}",
                    topic, key, event.getEventId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }
}