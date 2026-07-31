package com.eventflow.notification.infrastructure.persistence;

import com.eventflow.notification.domain.model.Notification;
import com.eventflow.notification.domain.model.Recipient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class JpaNotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaNotificationRepositoryAdapter repository;

    @Test
    void save_shouldPersistNotification() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .workspaceId(UUID.randomUUID())
                .templateId(UUID.randomUUID())
                .channel(Notification.Channel.EMAIL)
                .recipient(Recipient.builder().email("test@example.com").build())
                .status(Notification.Status.PENDING)
                .createdAt(Instant.now())
                .build();

        Notification saved = repository.save(notification);
        entityManager.flush();

        assertThat(saved.getId()).isEqualTo(notification.getId());
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void findById_shouldReturnNotification() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .workspaceId(UUID.randomUUID())
                .templateId(UUID.randomUUID())
                .channel(Notification.Channel.SMS)
                .recipient(Recipient.builder().phone("+1234567890").build())
                .status(Notification.Status.PROCESSING)
                .createdAt(Instant.now())
                .build();
        repository.save(notification);
        entityManager.flush();

        Optional<Notification> result = repository.findById(notification.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getChannel()).isEqualTo(Notification.Channel.SMS);
    }

    @Test
    void findByWorkspaceId_shouldReturnAllNotifications() {
        UUID workspaceId = UUID.randomUUID();
        
        Notification n1 = Notification.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .templateId(UUID.randomUUID())
                .channel(Notification.Channel.EMAIL)
                .recipient(Recipient.builder().email("user1@example.com").build())
                .status(Notification.Status.PENDING)
                .createdAt(Instant.now())
                .build();
        
        Notification n2 = Notification.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .templateId(UUID.randomUUID())
                .channel(Notification.Channel.SMS)
                .recipient(Recipient.builder().phone("+1234567890").build())
                .status(Notification.Status.DELIVERED)
                .createdAt(Instant.now())
                .build();

        repository.save(n1);
        repository.save(n2);
        entityManager.flush();

        List<Notification> results = repository.findByWorkspaceId(workspaceId);

        assertThat(results).hasSize(2);
    }

    @Test
    void findByStatus_shouldFilterByStatus() {
        UUID workspaceId = UUID.randomUUID();
        
        Notification pending = Notification.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .templateId(UUID.randomUUID())
                .channel(Notification.Channel.EMAIL)
                .recipient(Recipient.builder().email("test@example.com").build())
                .status(Notification.Status.PENDING)
                .createdAt(Instant.now())
                .build();
        
        Notification delivered = Notification.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .templateId(UUID.randomUUID())
                .channel(Notification.Channel.EMAIL)
                .recipient(Recipient.builder().email("test2@example.com").build())
                .status(Notification.Status.DELIVERED)
                .createdAt(Instant.now())
                .build();

        repository.save(pending);
        repository.save(delivered);
        entityManager.flush();

        List<Notification> results = repository.findByStatus(Notification.Status.PENDING);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(Notification.Status.PENDING);
    }

    @Test
    void updateStatus_shouldChangeStatus() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .workspaceId(UUID.randomUUID())
                .templateId(UUID.randomUUID())
                .channel(Notification.Channel.PUSH)
                .recipient(Recipient.builder().deviceToken("token123").build())
                .status(Notification.Status.PENDING)
                .createdAt(Instant.now())
                .build();
        repository.save(notification);
        entityManager.flush();
        entityManager.clear();

        notification.markAsProcessing();
        repository.save(notification);
        entityManager.flush();
        entityManager.clear();

        Notification updated = repository.findById(notification.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Notification.Status.PROCESSING);
    }

    @Test
    void findByWorkspaceIdAndChannel_shouldFilterByChannel() {
        UUID workspaceId = UUID.randomUUID();
        
        repository.save(Notification.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .templateId(UUID.randomUUID())
                .channel(Notification.Channel.EMAIL)
                .recipient(Recipient.builder().email("test@example.com").build())
                .status(Notification.Status.PENDING)
                .createdAt(Instant.now())
                .build());
        
        repository.save(Notification.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .templateId(UUID.randomUUID())
                .channel(Notification.Channel.SMS)
                .recipient(Recipient.builder().phone("+1234567890").build())
                .status(Notification.Status.PENDING)
                .createdAt(Instant.now())
                .build());
        
        entityManager.flush();

        List<Notification> emails = repository.findByWorkspaceIdAndChannel(workspaceId, Notification.Channel.EMAIL);

        assertThat(emails).hasSize(1);
        assertThat(emails.get(0).getChannel()).isEqualTo(Notification.Channel.EMAIL);
    }
}
