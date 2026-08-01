package com.eventflow.notification.infrastructure.scheduler;

import com.eventflow.notification.application.NotificationRepository;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationStatus;
import com.eventflow.notification.infrastructure.RetryScheduler;
import com.eventflow.common.infrastructure.KafkaEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for RetryScheduler.
 * Verifies that scheduled retries are properly processed.
 */
@ExtendWith(MockitoExtension.class)
class RetrySchedulerIntegrationTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private KafkaEventPublisher eventPublisher;

    @InjectMocks
    private RetryScheduler retryScheduler;

    @Test
    void shouldProcessScheduledRetries() {
        // Given
        Notification notification = mock(Notification.class);
        when(notification.getId()).thenReturn(UUID.randomUUID());
        when(notification.getStatus()).thenReturn(NotificationStatus.RETRY_SCHEDULED);
        when(notification.getNextRetryAt()).thenReturn(Instant.now().minusSeconds(60));

        when(notificationRepository.findScheduledRetries(any())).thenReturn(List.of(notification));

        // When
        retryScheduler.processRetries();

        // Then
        verify(notificationRepository, times(1)).findScheduledRetries(any());
        verify(eventPublisher, times(1)).publish(any(), any());
    }

    @Test
    void shouldHandleEmptyRetryQueue() {
        // Given
        when(notificationRepository.findScheduledRetries(any())).thenReturn(List.of());

        // When
        retryScheduler.processRetries();

        // Then
        verify(notificationRepository, times(1)).findScheduledRetries(any());
        verify(eventPublisher, never()).publish(any(), any());
    }
}
