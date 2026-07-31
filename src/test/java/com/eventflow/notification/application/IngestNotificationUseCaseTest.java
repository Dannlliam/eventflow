package com.eventflow.notification.application;

import com.eventflow.common.domain.Channel;
import com.eventflow.common.infrastructure.KafkaEventPublisher;
import com.eventflow.common.infrastructure.ObservabilityConfig;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationStatus;
import com.eventflow.notification.domain.Recipient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for IngestNotificationUseCase
 * Verifies notification ingestion, validation, persistence, and event publishing
 */
@ExtendWith(MockitoExtension.class)
class IngestNotificationUseCaseTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private KafkaEventPublisher eventPublisher;

    @Mock
    private ObservabilityConfig.NotificationMetrics notificationMetrics;

    private IngestNotificationUseCase useCase;

    private UUID workspaceId;
    private String templateSlug;
    private Map<String, Object> variables;

    @BeforeEach
    void setUp() {
        useCase = new IngestNotificationUseCase(
            notificationRepository,
            eventPublisher,
            notificationMetrics
        );

        workspaceId = UUID.randomUUID();
        templateSlug = "welcome-email";
        variables = Map.of("name", "John Doe", "email", "john@example.com");
    }

    @Test
    void shouldIngestEmailNotificationSuccessfully() {
        // Given
        Recipient recipient = new Recipient("john@example.com", null, null, null);
        Notification savedNotification = mock(Notification.class);
        when(savedNotification.getId()).thenReturn(UUID.randomUUID());
        when(savedNotification.getStatus()).thenReturn(NotificationStatus.QUEUED);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // When
        Notification result = useCase.execute(
            workspaceId,
            Channel.EMAIL,
            recipient,
            templateSlug,
            variables,
            Map.of()
        );

        // Then
        assertThat(result).isNotNull();
        verify(notificationRepository).save(any(Notification.class));
        verify(eventPublisher).publish(any());
        verify(notificationMetrics).recordIngestion();
        verify(notificationMetrics).recordPayloadSize(anyInt());
    }

    @Test
    void shouldIngestSmsNotificationSuccessfully() {
        // Given
        Recipient recipient = new Recipient(null, "+1234567890", null, null);
        Notification savedNotification = mock(Notification.class);
        when(savedNotification.getId()).thenReturn(UUID.randomUUID());
        when(savedNotification.getStatus()).thenReturn(NotificationStatus.QUEUED);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // When
        Notification result = useCase.execute(
            workspaceId,
            Channel.SMS,
            recipient,
            templateSlug,
            variables,
            Map.of()
        );

        // Then
        assertThat(result).isNotNull();
        verify(notificationRepository).save(any(Notification.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    void shouldSetInitialStatusToQueued() {
        // Given
        Recipient recipient = new Recipient("test@example.com", null, null, null);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(notificationCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        // When
        useCase.execute(workspaceId, Channel.EMAIL, recipient, templateSlug, variables, Map.of());

        // Then
        Notification captured = notificationCaptor.getValue();
        assertThat(captured.getStatus()).isEqualTo(NotificationStatus.QUEUED);
        assertThat(captured.getAttemptCount()).isEqualTo(0);
    }

    @Test
    void shouldPublishNotificationCreatedEvent() {
        // Given
        Recipient recipient = new Recipient("test@example.com", null, null, null);
        UUID notificationId = UUID.randomUUID();
        Notification savedNotification = mock(Notification.class);
        when(savedNotification.getId()).thenReturn(notificationId);
        when(savedNotification.getStatus()).thenReturn(NotificationStatus.QUEUED);
        when(notificationRepository.save(any())).thenReturn(savedNotification);

        // When
        useCase.execute(workspaceId, Channel.EMAIL, recipient, templateSlug, variables, Map.of());

        // Then
        verify(eventPublisher).publish(any());
    }

    @Test
    void shouldRecordMetrics() {
        // Given
        Recipient recipient = new Recipient("test@example.com", null, null, null);
        when(notificationRepository.save(any())).thenReturn(mock(Notification.class));

        // When
        useCase.execute(workspaceId, Channel.EMAIL, recipient, templateSlug, variables, Map.of());

        // Then
        verify(notificationMetrics).recordIngestion();
        verify(notificationMetrics).recordPayloadSize(anyInt());
    }

    @Test
    void shouldHandleMetadataCorrectly() {
        // Given
        Recipient recipient = new Recipient("test@example.com", null, null, null);
        Map<String, Object> metadata = Map.of("userId", "user123", "source", "web");
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(notificationCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        // When
        useCase.execute(workspaceId, Channel.EMAIL, recipient, templateSlug, variables, metadata);

        // Then
        Notification captured = notificationCaptor.getValue();
        assertThat(captured.getMetadata()).isEqualTo(metadata);
    }
}
