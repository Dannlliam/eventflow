package com.eventflow.notification.infrastructure.consumer;

import com.eventflow.notification.application.ProcessNotificationUseCase;
import com.eventflow.notification.domain.event.NotificationCreatedEvent;
import com.eventflow.notification.domain.model.Notification;
import com.eventflow.notification.domain.model.Recipient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationCreatedConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private ProcessNotificationUseCase processNotificationUseCase;

    @Test
    void onNotificationCreated_shouldProcessEvent() throws Exception {
        UUID notificationId = UUID.randomUUID();
        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .notificationId(notificationId)
                .workspaceId(UUID.randomUUID())
                .templateId(UUID.randomUUID())
                .channel(Notification.Channel.EMAIL)
                .recipient(Recipient.builder().email("test@example.com").build())
                .context(Map.of("name", "John"))
                .occurredAt(Instant.now())
                .build();

        Thread.sleep(2000);

        verify(processNotificationUseCase, timeout(5000).atLeastOnce()).execute(any());
    }
}
