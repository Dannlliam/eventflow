package com.eventflow.provider.infrastructure.dispatcher;

import com.eventflow.notification.domain.model.Notification;
import com.eventflow.provider.domain.model.Provider;
import com.eventflow.provider.domain.model.DispatchResult;
import com.eventflow.provider.infrastructure.dispatcher.email.SendGridEmailDispatcher;
import com.eventflow.provider.infrastructure.dispatcher.sms.TwilioSmsDispatcher;
import com.eventflow.provider.infrastructure.dispatcher.push.FcmPushDispatcher;
import com.eventflow.provider.infrastructure.dispatcher.webhook.WebhookDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositeNotificationDispatcherTest {

    @Mock
    private SendGridEmailDispatcher sendGridEmailDispatcher;

    @Mock
    private TwilioSmsDispatcher twilioSmsDispatcher;

    @Mock
    private FcmPushDispatcher fcmPushDispatcher;

    @Mock
    private WebhookDispatcher webhookDispatcher;

    private CompositeNotificationDispatcher compositeDispatcher;

    private UUID notificationId;
    private Provider emailProvider;
    private Provider smsProvider;

    @BeforeEach
    void setUp() {
        compositeDispatcher = new CompositeNotificationDispatcher(
                sendGridEmailDispatcher,
                twilioSmsDispatcher,
                fcmPushDispatcher,
                webhookDispatcher
        );

        notificationId = UUID.randomUUID();

        emailProvider = Provider.builder()
                .id(UUID.randomUUID())
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .status(Provider.Status.ACTIVE)
                .build();

        smsProvider = Provider.builder()
                .id(UUID.randomUUID())
                .type(Provider.ProviderType.TWILIO)
                .channel(Provider.Channel.SMS)
                .status(Provider.Status.ACTIVE)
                .build();
    }

    @Test
    void dispatch_shouldRouteEmailToSendGrid_successfully() {
        // Arrange
        Map<String, Object> content = Map.of(
                "subject", "Test Subject",
                "body", "Test Body"
        );

        DispatchResult expectedResult = DispatchResult.success(
                notificationId,
                emailProvider.getId(),
                "msg_12345"
        );

        when(sendGridEmailDispatcher.dispatch(eq(notificationId), eq(emailProvider), eq("user@example.com"), eq(content)))
                .thenReturn(expectedResult);

        // Act
        DispatchResult result = compositeDispatcher.dispatch(
                notificationId,
                emailProvider,
                "user@example.com",
                content
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExternalMessageId()).isEqualTo("msg_12345");

        verify(sendGridEmailDispatcher).dispatch(notificationId, emailProvider, "user@example.com", content);
        verify(twilioSmsDispatcher, never()).dispatch(any(), any(), any(), any());
        verify(fcmPushDispatcher, never()).dispatch(any(), any(), any(), any());
    }

    @Test
    void dispatch_shouldRouteSmsToTwilio_successfully() {
        // Arrange
        Map<String, Object> content = Map.of("body", "Your verification code is 123456");

        DispatchResult expectedResult = DispatchResult.success(
                notificationId,
                smsProvider.getId(),
                "SM9876543210"
        );

        when(twilioSmsDispatcher.dispatch(eq(notificationId), eq(smsProvider), eq("+1234567890"), eq(content)))
                .thenReturn(expectedResult);

        // Act
        DispatchResult result = compositeDispatcher.dispatch(
                notificationId,
                smsProvider,
                "+1234567890",
                content
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExternalMessageId()).isEqualTo("SM9876543210");

        verify(twilioSmsDispatcher).dispatch(notificationId, smsProvider, "+1234567890", content);
        verify(sendGridEmailDispatcher, never()).dispatch(any(), any(), any(), any());
    }

    @Test
    void dispatch_shouldHandleDispatchFailure() {
        // Arrange
        Map<String, Object> content = Map.of("subject", "Test", "body", "Body");

        DispatchResult failedResult = DispatchResult.failure(
                notificationId,
                emailProvider.getId(),
                "SMTP connection failed",
                new RuntimeException("Connection timeout")
        );

        when(sendGridEmailDispatcher.dispatch(any(), any(), any(), any()))
                .thenReturn(failedResult);

        // Act
        DispatchResult result = compositeDispatcher.dispatch(
                notificationId,
                emailProvider,
                "user@example.com",
                content
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("SMTP connection failed");
        assertThat(result.getException()).isNotNull();

        verify(sendGridEmailDispatcher).dispatch(notificationId, emailProvider, "user@example.com", content);
    }

    @Test
    void dispatch_shouldRoutePushToFcm() {
        // Arrange
        Provider pushProvider = Provider.builder()
                .id(UUID.randomUUID())
                .type(Provider.ProviderType.FCM)
                .channel(Provider.Channel.PUSH)
                .status(Provider.Status.ACTIVE)
                .build();

        Map<String, Object> content = Map.of(
                "title", "New Message",
                "body", "You have a new message"
        );

        DispatchResult expectedResult = DispatchResult.success(
                notificationId,
                pushProvider.getId(),
                "fcm_token_abc123"
        );

        when(fcmPushDispatcher.dispatch(eq(notificationId), eq(pushProvider), eq("device_token_xyz"), eq(content)))
                .thenReturn(expectedResult);

        // Act
        DispatchResult result = compositeDispatcher.dispatch(
                notificationId,
                pushProvider,
                "device_token_xyz",
                content
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();

        verify(fcmPushDispatcher).dispatch(notificationId, pushProvider, "device_token_xyz", content);
    }

    @Test
    void dispatch_shouldRouteWebhook() {
        // Arrange
        Provider webhookProvider = Provider.builder()
                .id(UUID.randomUUID())
                .type(Provider.ProviderType.WEBHOOK)
                .channel(Provider.Channel.WEBHOOK)
                .status(Provider.Status.ACTIVE)
                .build();

        Map<String, Object> content = Map.of(
                "event", "user.signup",
                "userId", "12345"
        );

        DispatchResult expectedResult = DispatchResult.success(
                notificationId,
                webhookProvider.getId(),
                "webhook_req_xyz"
        );

        when(webhookDispatcher.dispatch(eq(notificationId), eq(webhookProvider), eq("https://api.example.com/webhook"), eq(content)))
                .thenReturn(expectedResult);

        // Act
        DispatchResult result = compositeDispatcher.dispatch(
                notificationId,
                webhookProvider,
                "https://api.example.com/webhook",
                content
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();

        verify(webhookDispatcher).dispatch(notificationId, webhookProvider, "https://api.example.com/webhook", content);
    }

    @Test
    void dispatch_shouldThrowException_whenProviderTypeUnsupported() {
        // Arrange
        Provider unsupportedProvider = Provider.builder()
                .id(UUID.randomUUID())
                .type(Provider.ProviderType.CUSTOM) // Hypothetical unsupported type
                .channel(Provider.Channel.EMAIL)
                .status(Provider.Status.ACTIVE)
                .build();

        Map<String, Object> content = Map.of("subject", "Test");

        // Act & Assert
        assertThatThrownBy(() -> compositeDispatcher.dispatch(
                notificationId,
                unsupportedProvider,
                "user@example.com",
                content
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported provider type");

        verifyNoInteractions(sendGridEmailDispatcher, twilioSmsDispatcher, fcmPushDispatcher, webhookDispatcher);
    }

    @Test
    void dispatch_shouldThrowException_whenProviderIsNull() {
        // Arrange
        Map<String, Object> content = Map.of("subject", "Test");

        // Act & Assert
        assertThatThrownBy(() -> compositeDispatcher.dispatch(
                notificationId,
                null,
                "user@example.com",
                content
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider cannot be null");
    }

    @Test
    void dispatch_shouldThrowException_whenNotificationIdIsNull() {
        // Arrange
        Map<String, Object> content = Map.of("subject", "Test");

        // Act & Assert
        assertThatThrownBy(() -> compositeDispatcher.dispatch(
                null,
                emailProvider,
                "user@example.com",
                content
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Notification ID cannot be null");
    }

    @Test
    void dispatch_shouldThrowException_whenRecipientIsBlank() {
        // Arrange
        Map<String, Object> content = Map.of("subject", "Test");

        // Act & Assert
        assertThatThrownBy(() -> compositeDispatcher.dispatch(
                notificationId,
                emailProvider,
                "",
                content
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recipient address cannot be blank");
    }

    @Test
    void dispatch_shouldThrowException_whenContentIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> compositeDispatcher.dispatch(
                notificationId,
                emailProvider,
                "user@example.com",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content cannot be null");
    }

    @Test
    void dispatch_shouldMeasureLatency() {
        // Arrange
        Map<String, Object> content = Map.of("subject", "Test", "body", "Body");

        DispatchResult expectedResult = DispatchResult.success(
                notificationId,
                emailProvider.getId(),
                "msg_12345"
        );

        when(sendGridEmailDispatcher.dispatch(any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(100); // Simulate network delay
                    return expectedResult;
                });

        Instant before = Instant.now();

        // Act
        DispatchResult result = compositeDispatcher.dispatch(
                notificationId,
                emailProvider,
                "user@example.com",
                content
        );

        Instant after = Instant.now();

        // Assert
        assertThat(result.getLatencyMs()).isGreaterThanOrEqualTo(100);
        assertThat(result.getDispatchedAt()).isBetween(before, after);
    }

    @Test
    void dispatch_shouldCatchAndWrapExceptions() {
        // Arrange
        Map<String, Object> content = Map.of("subject", "Test");

        when(sendGridEmailDispatcher.dispatch(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Network error"));

        // Act
        DispatchResult result = compositeDispatcher.dispatch(
                notificationId,
                emailProvider,
                "user@example.com",
                content
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getException()).isNotNull();
        assertThat(result.getException().getMessage()).contains("Network error");
    }

    @Test
    void dispatch_shouldHandleProviderInactiveStatus() {
        // Arrange
        Provider inactiveProvider = Provider.builder()
                .id(UUID.randomUUID())
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .status(Provider.Status.INACTIVE)
                .build();

        Map<String, Object> content = Map.of("subject", "Test");

        // Act & Assert
        assertThatThrownBy(() -> compositeDispatcher.dispatch(
                notificationId,
                inactiveProvider,
                "user@example.com",
                content
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Provider is not active");
    }
}
