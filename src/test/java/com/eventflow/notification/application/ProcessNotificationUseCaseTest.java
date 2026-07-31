package com.eventflow.notification.application;

import com.eventflow.common.domain.Channel;
import com.eventflow.common.infrastructure.PhoneNumberNormalizationService;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationStatus;
import com.eventflow.notification.domain.Recipient;
import com.eventflow.notification.domain.events.NotificationCreatedEvent;
import com.eventflow.provider.application.ProviderRepository;
import com.eventflow.provider.domain.Provider;
import com.eventflow.provider.domain.ProviderType;
import com.eventflow.template.application.TemplateRendererPort;
import com.eventflow.template.application.TemplateRepository;
import com.eventflow.template.domain.RenderedContent;
import com.eventflow.template.domain.Template;
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
 * Unit tests for ProcessNotificationUseCase.
 * Tests the notification processing logic, template rendering,
 * provider resolution, and state machine transitions.
 */
@ExtendWith(MockitoExtension.class)
class ProcessNotificationUseCaseTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationEventRepository eventRepository;
    @Mock
    private NotificationEventPublisher eventPublisher;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateRendererPort templateRenderer;
    @Mock
    private ProviderRepository providerRepository;
    @Mock
    private PhoneNumberNormalizationService phoneNumberNormalization;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;
    @Captor
    private ArgumentCaptor<String> topicCaptor;
    @Captor
    private ArgumentCaptor<String> keyCaptor;
    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private ProcessNotificationUseCase useCase;
    private UUID workspaceId;
    private UUID notificationId;
    private Notification notification;
    private NotificationCreatedEvent createdEvent;
    private Provider emailProvider;

    @BeforeEach
    void setUp() {
        useCase = new ProcessNotificationUseCase(
            notificationRepository, eventRepository, eventPublisher,
            templateRepository, templateRenderer,
            providerRepository, phoneNumberNormalization
        );

        workspaceId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        Recipient recipient = new Recipient(
            "test@example.com",  // email
            "+15551234567",      // phone
            "device-token-123",  // deviceToken
            "https://hook.example.com/callback" // webhookUrl
        );

        Map<String, String> payload = Map.of("name", "John");

        notification = new Notification(
            workspaceId,
            Channel.EMAIL,
            recipient,
            "welcome-template",
            payload,
            Map.of("source", "test"),
            "idempotency-key-123"
        );

        createdEvent = notification.toCreatedEvent();

        emailProvider = new Provider(
            UUID.randomUUID(),
            workspaceId,
            "SendGrid Primary",
            ProviderType.SENDGRID,
            Channel.EMAIL,
            true,
            true,
            100,
            60,
            Map.of("apiKey", "sk-test"),
            Map.of(),
            Instant.now(),
            Instant.now(),
            0
        );
    }

    @Test
    void execute_shouldProcessQueuedNotification() {
        // Arrange
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(providerRepository.findPrimaryByWorkspaceIdAndChannel(
            eq(workspaceId), eq(Channel.EMAIL)))
            .thenReturn(Optional.of(emailProvider));

        // Act
        ProcessNotificationUseCase.ProcessingResult result =
            useCase.execute(notificationId, createdEvent);

        // Assert
        assertTrue(result.success());
        assertEquals(NotificationStatus.DISPATCHED, result.status());

        // Verify state machine transitions
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        List<Notification> savedNotifications = notificationCaptor.getAllValues();
        assertEquals(NotificationStatus.PROCESSING, savedNotifications.get(0).getStatus());
        assertEquals(NotificationStatus.DISPATCHED, savedNotifications.get(1).getStatus());

        // Verify dispatch event published
        verify(eventPublisher).publish(
            eq("dispatch.requested"),
            eq(notificationId.toString()),
            any()
        );
    }

    @Test
    void execute_shouldRejectNonQueuedStatus() {
        // Arrange
        notification.markProcessing();
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));

        // Act
        ProcessNotificationUseCase.ProcessingResult result =
            useCase.execute(notificationId, createdEvent);

        // Assert
        assertFalse(result.success());
        assertEquals(NotificationStatus.PROCESSING, result.status());
        verify(notificationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void execute_shouldFallbackToSecondaryProvider() {
        // Arrange
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Primary unavailable, fallback to secondary
        when(providerRepository.findPrimaryByWorkspaceIdAndChannel(
            eq(workspaceId), eq(Channel.EMAIL)))
            .thenReturn(Optional.empty());
        when(providerRepository.findFirstByWorkspaceIdAndChannelAndEnabled(
            eq(workspaceId), eq(Channel.EMAIL)))
            .thenReturn(Optional.of(emailProvider));

        // Act
        ProcessNotificationUseCase.ProcessingResult result =
            useCase.execute(notificationId, createdEvent);

        // Assert
        assertTrue(result.success());
        verify(eventPublisher).publish(eq("dispatch.requested"), any(), any());
    }

    @Test
    void execute_shouldFailWhenNoProviderFound() {
        // Arrange
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(providerRepository.findPrimaryByWorkspaceIdAndChannel(
            eq(workspaceId), eq(Channel.EMAIL)))
            .thenReturn(Optional.empty());
        when(providerRepository.findFirstByWorkspaceIdAndChannelAndEnabled(
            eq(workspaceId), eq(Channel.EMAIL)))
            .thenReturn(Optional.empty());

        // Act
        ProcessNotificationUseCase.ProcessingResult result =
            useCase.execute(notificationId, createdEvent);

        // Assert
        assertFalse(result.success());
        assertEquals(NotificationStatus.FAILED, result.status());
        verify(notificationRepository, times(2)).save(any());
        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void execute_shouldRenderTemplate() {
        // Arrange
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Template.TemplateVersion activeVersion = 
            new Template.TemplateVersion(
                UUID.randomUUID(),
                "welcome-template",
                1,
                Channel.EMAIL,
                "Welcome {{name}}!",
                "<h1>Hello {{name}}</h1>",
                true,
                UUID.randomUUID()
            );
        Template template = mock(Template.class);
        when(template.getActiveVersion()).thenReturn(Optional.of(activeVersion));
        when(templateRepository.findBySlug("welcome-template"))
            .thenReturn(Optional.of(template));
        when(templateRenderer.render(
            eq(activeVersion.getBodyTemplate()),
            eq(activeVersion.getSubjectTemplate()),
            any()
        )).thenReturn(new RenderedContent(
            "<h1>Hello John</h1>",
            "Welcome John!",
            "Welcome John!"
        ));
        when(providerRepository.findPrimaryByWorkspaceIdAndChannel(
            eq(workspaceId), eq(Channel.EMAIL)))
            .thenReturn(Optional.of(emailProvider));

        // Act
        ProcessNotificationUseCase.ProcessingResult result =
            useCase.execute(notificationId, createdEvent);

        // Assert
        assertTrue(result.success());
        verify(templateRenderer).render(any(), any(), any());
    }

    @Test
    void execute_shouldFailWithInvalidRecipient() {
        // Arrange: notification with null recipient email
        Recipient invalidRecipient = new Recipient(null, null, null, null);
        Notification invalidNotification = new Notification(
            workspaceId, Channel.EMAIL, invalidRecipient,
            "test", Map.of(), Map.of(), null
        );
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(invalidNotification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProcessNotificationUseCase.ProcessingResult result =
            useCase.execute(notificationId, 
                invalidNotification.toCreatedEvent());

        // Assert
        assertFalse(result.success());
        assertEquals(NotificationStatus.FAILED, result.status());
        verify(notificationRepository, times(2)).save(any());
        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void execute_shouldThrowWhenNotificationNotFound() {
        // Arrange
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            useCase.execute(notificationId, createdEvent));
        verify(notificationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void execute_shouldReturnProcessingResultWithCorrectStatus() {
        // Arrange
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(providerRepository.findPrimaryByWorkspaceIdAndChannel(
            eq(workspaceId), eq(Channel.EMAIL)))
            .thenReturn(Optional.of(emailProvider));

        // Act
        ProcessNotificationUseCase.ProcessingResult result =
            useCase.execute(notificationId, createdEvent);

        // Assert
        assertNotNull(result);
        assertEquals(notificationId, result.notificationId());
        assertEquals(NotificationStatus.DISPATCHED, result.status());
        assertNull(result.errorMessage());
    }
}