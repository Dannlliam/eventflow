package com.eventflow.common.infrastructure.kafka;

import com.eventflow.common.domain.event.DomainEvent;
import com.eventflow.notification.domain.event.NotificationCreatedEvent;
import com.eventflow.notification.domain.event.DispatchRequestedEvent;
import com.eventflow.notification.domain.model.Notification;
import com.eventflow.notification.domain.model.Recipient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaEventPublisher kafkaEventPublisher;

    private UUID notificationId;
    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        kafkaEventPublisher = new KafkaEventPublisher(kafkaTemplate);
        notificationId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
    }

    @Test
    void publish_shouldSendNotificationCreatedEvent_successfully() {
        // Arrange
        Recipient recipient = Recipient.builder()
                .email("user@example.com")
                .build();

        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .notificationId(notificationId)
                .workspaceId(workspaceId)
                .channel(Notification.Channel.EMAIL)
                .templateId(UUID.randomUUID())
                .recipient(recipient)
                .context(java.util.Map.of("key", "value"))
                .occurredAt(Instant.now())
                .build();

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("notification.created"), eq(notificationId.toString()), anyString()))
                .thenReturn(future);

        // Act
        kafkaEventPublisher.publish(event);

        // Assert
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), messageCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("notification.created");
        assertThat(keyCaptor.getValue()).isEqualTo(notificationId.toString());
        assertThat(messageCaptor.getValue()).contains("\"notificationId\"");
        assertThat(messageCaptor.getValue()).contains(notificationId.toString());
    }

    @Test
    void publish_shouldSendDispatchRequestedEvent_successfully() {
        // Arrange
        UUID providerId = UUID.randomUUID();
        
        DispatchRequestedEvent event = DispatchRequestedEvent.builder()
                .notificationId(notificationId)
                .workspaceId(workspaceId)
                .providerId(providerId)
                .channel(Notification.Channel.EMAIL)
                .recipientAddress("user@example.com")
                .renderedContent(java.util.Map.of("subject", "Test", "body", "Body"))
                .occurredAt(Instant.now())
                .build();

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("dispatch.requested"), eq(notificationId.toString()), anyString()))
                .thenReturn(future);

        // Act
        kafkaEventPublisher.publish(event);

        // Assert
        verify(kafkaTemplate).send(eq("dispatch.requested"), eq(notificationId.toString()), anyString());
    }

    @Test
    void publish_shouldUseNotificationIdAsPartitionKey() {
        // Arrange
        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .notificationId(notificationId)
                .workspaceId(workspaceId)
                .channel(Notification.Channel.EMAIL)
                .templateId(UUID.randomUUID())
                .recipient(Recipient.builder().email("user@example.com").build())
                .context(java.util.Map.of())
                .occurredAt(Instant.now())
                .build();

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // Act
        kafkaEventPublisher.publish(event);

        // Assert
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), keyCaptor.capture(), anyString());
        
        assertThat(keyCaptor.getValue()).isEqualTo(notificationId.toString());
    }

    @Test
    void publish_shouldSerializeEventToJson() {
        // Arrange
        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .notificationId(notificationId)
                .workspaceId(workspaceId)
                .channel(Notification.Channel.EMAIL)
                .templateId(UUID.randomUUID())
                .recipient(Recipient.builder().email("user@example.com").build())
                .context(java.util.Map.of("firstName", "John", "lastName", "Doe"))
                .occurredAt(Instant.now())
                .build();

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // Act
        kafkaEventPublisher.publish(event);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), anyString(), messageCaptor.capture());
        
        String message = messageCaptor.getValue();
        assertThat(message).contains("\"notificationId\"");
        assertThat(message).contains("\"workspaceId\"");
        assertThat(message).contains("\"channel\"");
        assertThat(message).contains("\"firstName\":\"John\"");
        assertThat(message).contains("\"lastName\":\"Doe\"");
    }

    @Test
    void publish_shouldRouteToCorrectTopic_basedOnEventType() {
        // Arrange
        NotificationCreatedEvent createdEvent = NotificationCreatedEvent.builder()
                .notificationId(notificationId)
                .workspaceId(workspaceId)
                .channel(Notification.Channel.EMAIL)
                .templateId(UUID.randomUUID())
                .recipient(Recipient.builder().email("user@example.com").build())
                .context(java.util.Map.of())
                .occurredAt(Instant.now())
                .build();

        DispatchRequestedEvent dispatchEvent = DispatchRequestedEvent.builder()
                .notificationId(notificationId)
                .workspaceId(workspaceId)
                .providerId(UUID.randomUUID())
                .channel(Notification.Channel.EMAIL)
                .recipientAddress("user@example.com")
                .renderedContent(java.util.Map.of())
                .occurredAt(Instant.now())
                .build();

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // Act
        kafkaEventPublisher.publish(createdEvent);
        kafkaEventPublisher.publish(dispatchEvent);

        // Assert
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), anyString(), anyString());
        
        assertThat(topicCaptor.getAllValues()).containsExactly(
                "notification.created",
                "dispatch.requested"
        );
    }

    @Test
    void publish_shouldThrowException_whenEventIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> kafkaEventPublisher.publish(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event cannot be null");

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void publish_shouldHandleKafkaFailure_gracefully() {
        // Arrange
        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .notificationId(notificationId)
                .workspaceId(workspaceId)
                .channel(Notification.Channel.EMAIL)
                .templateId(UUID.randomUUID())
                .recipient(Recipient.builder().email("user@example.com").build())
                .context(java.util.Map.of())
                .occurredAt(Instant.now())
                .build();

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker unavailable"));
        
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

        // Act & Assert
        // Should not throw - error handling should be async
        kafkaEventPublisher.publish(event);

        verify(kafkaTemplate).send(anyString(), anyString(), anyString());
    }

    @Test
    void publish_shouldIncludeEventMetadata() {
        // Arrange
        Instant occurredAt = Instant.parse("2026-07-31T10:00:00Z");
        
        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .notificationId(notificationId)
                .workspaceId(workspaceId)
                .channel(Notification.Channel.EMAIL)
                .templateId(UUID.randomUUID())
                .recipient(Recipient.builder().email("user@example.com").build())
                .context(java.util.Map.of())
                .occurredAt(occurredAt)
                .build();

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // Act
        kafkaEventPublisher.publish(event);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), anyString(), messageCaptor.capture());
        
        String message = messageCaptor.getValue();
        assertThat(message).contains("\"occurredAt\"");
        assertThat(message).contains("2026-07-31");
    }

    @Test
    void publish_shouldHandleMultipleChannels() {
        // Arrange
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // Act - Test EMAIL
        NotificationCreatedEvent emailEvent = NotificationCreatedEvent.builder()
                .notificationId(UUID.randomUUID())
                .workspaceId(workspaceId)
                .channel(Notification.Channel.EMAIL)
                .templateId(UUID.randomUUID())
                .recipient(Recipient.builder().email("user@example.com").build())
                .context(java.util.Map.of())
                .occurredAt(Instant.now())
                .build();
        kafkaEventPublisher.publish(emailEvent);

        // Act - Test SMS
        NotificationCreatedEvent smsEvent = NotificationCreatedEvent.builder()
                .notificationId(UUID.randomUUID())
                .workspaceId(workspaceId)
                .channel(Notification.Channel.SMS)
                .templateId(UUID.randomUUID())
                .recipient(Recipient.builder().phone("+1234567890").build())
                .context(java.util.Map.of())
                .occurredAt(Instant.now())
                .build();
        kafkaEventPublisher.publish(smsEvent);

        // Act - Test PUSH
        NotificationCreatedEvent pushEvent = NotificationCreatedEvent.builder()
                .notificationId(UUID.randomUUID())
                .workspaceId(workspaceId)
                .channel(Notification.Channel.PUSH)
                .templateId(UUID.randomUUID())
                .recipient(Recipient.builder().deviceToken("device-token").build())
                .context(java.util.Map.of())
                .occurredAt(Instant.now())
                .build();
        kafkaEventPublisher.publish(pushEvent);

        // Assert
        verify(kafkaTemplate, times(3)).send(anyString(), anyString(), anyString());
    }

    @Test
    void publish_shouldPreserveContextData() {
        // Arrange
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        context.put("orderId", "12345");
        context.put("amount", 99.99);
        context.put("customerName", "John Doe");
        
        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .notificationId(notificationId)
                .workspaceId(workspaceId)
                .channel(Notification.Channel.EMAIL)
                .templateId(UUID.randomUUID())
                .recipient(Recipient.builder().email("user@example.com").build())
                .context(context)
                .occurredAt(Instant.now())
                .build();

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // Act
        kafkaEventPublisher.publish(event);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), anyString(), messageCaptor.capture());
        
        String message = messageCaptor.getValue();
        assertThat(message).contains("\"orderId\":\"12345\"");
        assertThat(message).contains("\"amount\":99.99");
        assertThat(message).contains("\"customerName\":\"John Doe\"");
    }
}
