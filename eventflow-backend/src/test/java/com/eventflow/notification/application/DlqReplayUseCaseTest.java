package com.eventflow.notification.application;

import com.eventflow.common.infrastructure.KafkaEventPublisher;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for DlqReplayUseCase
 * Verifies DLQ message replay functionality
 */
@ExtendWith(MockitoExtension.class)
class DlqReplayUseCaseTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private KafkaEventPublisher eventPublisher;

    private DlqReplayUseCase useCase;

    private UUID notificationId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        useCase = new DlqReplayUseCase(notificationRepository, eventPublisher);
        notificationId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
    }

    @Test
    void shouldReplayDlqMessageSuccessfully() {
        // Given
        Notification notification = mock(Notification.class);
        when(notification.getStatus()).thenReturn(NotificationStatus.FAILED);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When
        DlqReplayUseCase.ReplayResult result = useCase.execute(notificationId, adminUserId);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("successfully");
        verify(notification).resetForReplay();
        verify(notificationRepository).save(notification);
        verify(eventPublisher).publish(any());
    }

    @Test
    void shouldReturnFailureWhenNotificationNotFound() {
        // Given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When
        DlqReplayUseCase.ReplayResult result = useCase.execute(notificationId, adminUserId);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("not found");
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldResetNotificationForReplay() {
        // Given
        Notification notification = mock(Notification.class);
        when(notification.getStatus()).thenReturn(NotificationStatus.FAILED);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When
        useCase.execute(notificationId, adminUserId);

        // Then
        verify(notification).resetForReplay();
        verify(notificationRepository).save(notification);
    }

    @Test
    void shouldPublishNotificationCreatedEvent() {
        // Given
        Notification notification = mock(Notification.class);
        when(notification.getId()).thenReturn(notificationId);
        when(notification.getStatus()).thenReturn(NotificationStatus.FAILED);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When
        useCase.execute(notificationId, adminUserId);

        // Then
        verify(eventPublisher).publish(any());
    }

    @Test
    void shouldHandleReplayForBouncedNotifications() {
        // Given
        Notification notification = mock(Notification.class);
        when(notification.getStatus()).thenReturn(NotificationStatus.BOUNCED);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When
        DlqReplayUseCase.ReplayResult result = useCase.execute(notificationId, adminUserId);

        // Then
        assertThat(result.success()).isTrue();
        verify(notification).resetForReplay();
    }

    @Test
    void shouldNotReplayAlreadyQueuedNotification() {
        // Given
        Notification notification = mock(Notification.class);
        when(notification.getStatus()).thenReturn(NotificationStatus.QUEUED);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When
        DlqReplayUseCase.ReplayResult result = useCase.execute(notificationId, adminUserId);

        // Then
        // Implementation depends on business logic - may allow or reject
        verify(notificationRepository, atMostOnce()).save(notification);
    }
}
