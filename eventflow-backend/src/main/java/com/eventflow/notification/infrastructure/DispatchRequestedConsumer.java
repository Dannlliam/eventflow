package com.eventflow.notification.infrastructure;

import com.eventflow.notification.application.NotificationEventPublisher;
import com.eventflow.notification.application.NotificationRepository;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.events.DispatchRequestedEvent;
import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.eventflow.provider.application.NotificationDispatcherPort;
import com.eventflow.provider.domain.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for processing dispatch.requested events.
 * Invokes the appropriate provider adapter and publishes the result.
 */
@Component
public class DispatchRequestedConsumer {

    private static final Logger log = LoggerFactory.getLogger(DispatchRequestedConsumer.class);

    private final NotificationDispatcherPort notificationDispatcher;
    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public DispatchRequestedConsumer(NotificationDispatcherPort notificationDispatcher,
                                     NotificationRepository notificationRepository,
                                     NotificationEventPublisher eventPublisher,
                                     TransactionTemplate transactionTemplate) {
        this.notificationDispatcher = notificationDispatcher;
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    @KafkaListener(
        topics = "dispatch.requested",
        groupId = "eventflow-dispatch-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onDispatchRequested(DispatchRequestedEvent event,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                    @Header(KafkaHeaders.OFFSET) long offset,
                                    Acknowledgment ack) {
        log.info("Processing dispatch request: eventId={}, notificationId={}, providerType={}",
            event.getEventId(), event.notificationId(), event.providerType());

        try {
            // Create additional headers for the provider
            Map<String, String> additionalHeaders = Map.of(
                "workspaceId", event.workspaceId().toString(),
                "notificationId", event.notificationId().toString()
            );

            // Invoke the provider adapter
            ProviderType providerType = ProviderType.fromString(event.providerType());
            DispatchResultEvent result = notificationDispatcher.dispatch(
                providerType,
                event.recipient(),
                event.subject(),
                event.body(),
                event.notificationId().toString(),
                additionalHeaders
            );

            // Publish the result back to Kafka
            eventPublisher.publishDispatchResult(result);

            log.info("Dispatch completed: notificationId={}, providerType={}, success={}",
                event.notificationId(), event.providerType(), result.success());

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing dispatch request: eventId={}", event.getEventId(), e);
            // Publish a transient failure result so the retry mechanism can handle it
            DispatchResultEvent failureResult = DispatchResultEvent.transientFailure(
                event.notificationId(),
                event.workspaceId(),
                event.providerType(),
                0,
                "Consumer error: " + e.getMessage()
            );
            eventPublisher.publishDispatchResult(failureResult);
            throw e;
        }
    }
}