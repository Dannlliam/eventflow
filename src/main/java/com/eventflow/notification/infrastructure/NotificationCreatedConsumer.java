package com.eventflow.notification.infrastructure;

import com.eventflow.notification.application.ProcessNotificationUseCase;
import com.eventflow.notification.domain.events.NotificationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for processing notification.created events.
 * Delegates all processing logic to {@link ProcessNotificationUseCase}
 * to avoid duplication and ensure consistent state machine transitions.
 */
@Component
public class NotificationCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationCreatedConsumer.class);

    private final ProcessNotificationUseCase processNotificationUseCase;

    public NotificationCreatedConsumer(ProcessNotificationUseCase processNotificationUseCase) {
        this.processNotificationUseCase = processNotificationUseCase;
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
            processNotificationUseCase.execute(event.notificationId(), event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing notification created event: eventId={}", event.getEventId(), e);
            // Error handler will manage retries and DLQ via kafkaErrorHandler
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
}