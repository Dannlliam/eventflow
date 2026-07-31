package com.eventflow.notification.application;

import com.eventflow.common.infrastructure.EventFlowProperties;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationStatus;
import com.eventflow.notification.domain.Recipient;
import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.eventflow.common.domain.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HandleDispatchResultUseCase.
 * Tests the dispatch result handling, retry scheduling, and DLQ routing.
 */
@ExtendWith(MockitoExtension.class)
class HandleDispatchResultUseCaseTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationEventRepository eventRepository;
    @Mock
    private NotificationEventPublisher eventPublisher;
    @Mock
    private EventFlowProperties eventFlowProperties;
    @Mock
    private EventFlowProperties.RetryProperties retryProperties;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private HandleDispatchResultUseCase useCase;
    private UUID workspaceId;
    private UUID notificationId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        useCase = new HandleDispatchResultUseCase(
            notificationRepository, eventRepository, eventPublisher, eventFlowProperties
        );

        workspaceId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        Recipient recipient = new Recipient(
            "test@example.com", null, null, null
        );

        notification = new Notification(
            workspaceId,
            Channel.EMAIL,
            recipient,
            "test-template",
            Map.of(),
            Map.of(),
            null
        );
    }

    @Test
    void execute_shouldDeliverOnSuccess() {
        // Arrange
        notification.markDispatched(UUID.randomUUID());
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        DispatchResultEvent successEvent = DispatchResultEvent.success(
            notificationId, workspaceId, "SENDGRID", 200, "OK"
        );

        // Act
        HandleDispatchResultUseCase.DispatchHandlingResult result =
            useCase.execute(successEvent);

        // Assert
        assertTrue(result.success());
        assertEquals(NotificationStatus.DELIVERED, result.status());
        verify(notificationRepository).save(notificationCaptor.capture());
        assertEquals(NotificationStatus.DELIVERED,
            notificationCaptor.getValue().getStatus());
    }

    @Test
    void execute_shouldScheduleRetryOnTransientFailure() {
        // Arrange
        notification.markDispatched(UUID.randomUUID());
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventFlowProperties.getRetry()).thenReturn(retryProperties);
        when(retryProperties.getMaxAttempts()).thenReturn(5);
        when(retryProperties.getBaseDelayMs()).thenReturn(60000L);
        when(retryProperties.getMultiplier()).thenReturn(2);
        when(retryProperties.getJitterPercentage()).thenReturn(0.2);

        DispatchResultEvent transientFailure = DispatchResultEvent.transientFailure(
            notificationId, workspaceId, "SENDGRID", 503, "Service Unavailable"
        );

        // Act
        HandleDispatchResultUseCase.DispatchHandlingResult result =
            useCase.execute(transientFailure);

        // Assert
        assertTrue(result.success());
        assertEquals(NotificationStatus.RETRY_SCHEDULED, result.status());
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertEquals(NotificationStatus.RETRY_SCHEDULED, saved.getStatus());
        assertTrue(saved.getNextRetryAt().isPresent());
        assertTrue(saved.getNextRetryAt().get().isAfter(Instant.now()));
    }

    @Test
    void execute_shouldRouteToDlqOnMaxRetriesExceeded() {
        // Arrange
        notification.markDispatched(UUID.randomUUID());
        // Exhaust max retries
        notification.incrementAttempt();
        notification.incrementAttempt();
        notification.incrementAttempt();
        notification.incrementAttempt();
        notification.incrementAttempt();

        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventFlowProperties.getRetry()).thenReturn(retryProperties);
        when(retryProperties.getMaxAttempts()).thenReturn(5);

        DispatchResultEvent transientFailure = DispatchResultEvent.transientFailure(
            notificationId, workspaceId, "SENDGRID", 503, "Service Unavailable"
        );

        // Act
        HandleDispatchResultUseCase.DispatchHandlingResult result =
            useCase.execute(transientFailure);

        // Assert
        assertFalse(result.success());
        assertEquals(NotificationStatus.DLQ, result.status());
        verify(notificationRepository).save(notificationCaptor.capture());
        assertEquals(NotificationStatus.DLQ,
            notificationCaptor.getValue().getStatus());
    }

    @Test
    void execute_shouldRouteToDlqOnPermanentFailure() {
        // Arrange
        notification.markDispatched(UUID.randomUUID());
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        DispatchResultEvent permanentFailure = DispatchResultEvent.permanentFailure(
            notificationId, workspaceId, "SENDGRID", 400, "Bad Request - Invalid payload"
        );

        // Act
        HandleDispatchResultUseCase.DispatchHandlingResult result =
            useCase.execute(permanentFailure);

        // Assert
        assertFalse(result.success());
        assertEquals(NotificationStatus.DLQ, result.status());
        verify(notificationRepository).save(notificationCaptor.capture());
        assertEquals(NotificationStatus.DLQ,
            notificationCaptor.getValue().getStatus());
    }

    @Test
    void execute_shouldRejectUnexpectedState() {
        // Arrange
        // Notification still in QUEUED state (not DISPATCHED)
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));

        DispatchResultEvent successEvent = DispatchResultEvent.success(
            notificationId, workspaceId, "SENDGRID", 200, "OK"
        );

        // Act
        HandleDispatchResultUseCase.DispatchHandlingResult result =
            useCase.execute(successEvent);

        // Assert
        assertFalse(result.success());
        assertEquals(NotificationStatus.QUEUED, result.status());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowWhenNotificationNotFound() {
        // Arrange
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.empty());

        DispatchResultEvent event = DispatchResultEvent.success(
            notificationId, workspaceId, "SENDGRID", 200, "OK"
        );

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            useCase.execute(event));
    }
}