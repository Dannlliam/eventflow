package com.eventflow.notification.infrastructure.consumer;

import com.eventflow.notification.application.HandleDispatchResultUseCase;
import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.eventflow.notification.infrastructure.DispatchResultConsumer;
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
 * Integration test for DispatchResultConsumer.
 * Verifies that dispatch result events are properly consumed and processed.
 */
@ExtendWith(MockitoExtension.class)
class DispatchResultConsumerIntegrationTest {

    @Mock
    private HandleDispatchResultUseCase handleDispatchResultUseCase;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private DispatchResultConsumer consumer;

    @Test
    void shouldHandleSuccessfulDispatchResult() {
        // Given
        UUID notificationId = UUID.randomUUID();
        DispatchResultEvent event = new DispatchResultEvent(
            UUID.randomUUID().toString(),
            notificationId,
            "SENDGRID",
            true,
            null,
            Map.of("messageId", "msg_123")
        );

        // When
        consumer.onDispatchResult(event, 0, 123L, acknowledgment);

        // Then
        verify(handleDispatchResultUseCase, times(1)).execute(any());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void shouldHandleFailedDispatchResult() {
        // Given
        UUID notificationId = UUID.randomUUID();
        DispatchResultEvent event = new DispatchResultEvent(
            UUID.randomUUID().toString(),
            notificationId,
            "SENDGRID",
            false,
            "Provider timeout",
            Map.of()
        );

        // When
        consumer.onDispatchResult(event, 0, 123L, acknowledgment);

        // Then
        verify(handleDispatchResultUseCase, times(1)).execute(any());
        verify(acknowledgment, times(1)).acknowledge();
    }
}
