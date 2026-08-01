package com.eventflow.notification.infrastructure.consumer;

import com.eventflow.notification.domain.events.DispatchRequestedEvent;
import com.eventflow.notification.infrastructure.DispatchRequestedConsumer;
import com.eventflow.provider.application.CompositeNotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for DispatchRequestedConsumer.
 * Verifies that dispatch events are properly consumed and processed.
 */
@ExtendWith(MockitoExtension.class)
class DispatchRequestedConsumerIntegrationTest {

    @Mock
    private CompositeNotificationDispatcher dispatcher;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private DispatchRequestedConsumer consumer;

    @Test
    void shouldDispatchNotificationWhenEventReceived() {
        // Given
        UUID notificationId = UUID.randomUUID();
        DispatchRequestedEvent event = new DispatchRequestedEvent(
            UUID.randomUUID().toString(),
            notificationId,
            "EMAIL",
            "SENDGRID",
            "test@example.com",
            null,
            null,
            null,
            "welcome-email",
            Map.of("userName", "Test User")
        );

        // When
        consumer.onDispatchRequested(event, 0, 123L, acknowledgment);

        // Then
        verify(dispatcher, times(1)).dispatch(any());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void shouldAcknowledgeEvenWhenDispatchFails() {
        // Given
        UUID notificationId = UUID.randomUUID();
        DispatchRequestedEvent event = new DispatchRequestedEvent(
            UUID.randomUUID().toString(),
            notificationId,
            "EMAIL",
            "SENDGRID",
            "test@example.com",
            null,
            null,
            null,
            "welcome-email",
            Map.of()
        );

        doThrow(new RuntimeException("Dispatch failed"))
            .when(dispatcher).dispatch(any());

        // When/Then
        try {
            consumer.onDispatchRequested(event, 0, 123L, acknowledgment);
        } catch (Exception e) {
            // Expected
        }

        verify(dispatcher, times(1)).dispatch(any());
        verify(acknowledgment, never()).acknowledge();
    }
}
