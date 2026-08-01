package com.eventflow.notification.infrastructure;

import com.eventflow.common.domain.Channel;
import com.eventflow.notification.domain.Notification;
import com.eventflow.notification.domain.NotificationStatus;
import com.eventflow.notification.domain.Recipient;
import com.eventflow.notification.application.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the JPA NotificationRepository.
 * Tests database interaction with real PostgreSQL via Testcontainers.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaNotificationRepository.class, ObjectMapper.class})
@ActiveProfiles("test")
@DisplayName("NotificationRepository Integration Test")
class NotificationRepositoryIntegrationTest {

    @Autowired
    private SpringDataNotificationRepository springDataRepository;

    private NotificationRepository notificationRepository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        notificationRepository = new JpaNotificationRepository(springDataRepository, objectMapper);
    }

    @Test
    @DisplayName("should save and retrieve a notification")
    void saveAndFindById_ValidNotification_ReturnsSavedNotification() {
        UUID workspaceId = UUID.randomUUID();
        Recipient recipient = new Recipient("user@example.com", null, null, null);
        Notification notification = new Notification(
            workspaceId, Channel.EMAIL, recipient, "welcome-email",
            Map.of("name", "John"), null, "idem-123");

        Notification saved = notificationRepository.save(notification);
        assertNotNull(saved.getId());

        Optional<Notification> found = notificationRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(workspaceId, found.get().getWorkspaceId());
        assertEquals(Channel.EMAIL, found.get().getChannel());
        assertEquals("user@example.com", found.get().getRecipient().email());
        assertEquals(NotificationStatus.QUEUED, found.get().getStatus());
        assertEquals("welcome-email", found.get().getTemplateSlug().orElse(null));
    }

    @Test
    @DisplayName("should find notification by workspace ID and ID")
    void findByIdAndWorkspaceId_ValidIds_ReturnsNotification() {
        UUID workspaceId = UUID.randomUUID();
        Notification notification = new Notification(
            workspaceId, Channel.SMS,
            new Recipient(null, "+1234567890", null, null),
            null, Map.of(), null, null);

        Notification saved = notificationRepository.save(notification);

        Optional<Notification> found = notificationRepository.findByIdAndWorkspaceId(
            saved.getId(), workspaceId);
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    @DisplayName("should not find notification by wrong workspace ID")
    void findByIdAndWorkspaceId_WrongWorkspaceId_ReturnsEmpty() {
        UUID workspaceId = UUID.randomUUID();
        Notification notification = new Notification(
            workspaceId, Channel.EMAIL,
            new Recipient("test@example.com", null, null, null),
            null, Map.of(), null, null);

        Notification saved = notificationRepository.save(notification);

        Optional<Notification> found = notificationRepository.findByIdAndWorkspaceId(
            saved.getId(), UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("should find notification by idempotency key")
    void findByIdempotencyKey_ExistingKey_ReturnsNotification() {
        UUID workspaceId = UUID.randomUUID();
        String idempotencyKey = "unique-key-123";

        Notification notification = new Notification(
            workspaceId, Channel.EMAIL,
            new Recipient("user@example.com", null, null, null),
            null, Map.of(), null, idempotencyKey);

        notificationRepository.save(notification);

        Optional<Notification> found = notificationRepository.findByIdempotencyKey(
            workspaceId, idempotencyKey);
        assertTrue(found.isPresent());
    }

    @Test
    @DisplayName("should update notification status")
    void updateStatus_ValidId_UpdatesStatus() {
        Notification notification = new Notification(
            UUID.randomUUID(), Channel.EMAIL,
            new Recipient("user@example.com", null, null, null),
            null, Map.of(), null, null);

        Notification saved = notificationRepository.save(notification);
        assertEquals(NotificationStatus.QUEUED, saved.getStatus());

        notificationRepository.updateStatus(saved.getId(), NotificationStatus.PROCESSING);

        Optional<Notification> updated = notificationRepository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals(NotificationStatus.PROCESSING, updated.get().getStatus());
    }

    @Test
    @DisplayName("should count notifications by status")
    void countByStatus_ReturnsCorrectCount() {
        UUID workspaceId = UUID.randomUUID();
        notificationRepository.save(new Notification(
            workspaceId, Channel.EMAIL,
            new Recipient("a@example.com", null, null, null),
            null, Map.of(), null, null));
        notificationRepository.save(new Notification(
            workspaceId, Channel.EMAIL,
            new Recipient("b@example.com", null, null, null),
            null, Map.of(), null, null));

        long count = notificationRepository.countByStatus(NotificationStatus.QUEUED);
        assertEquals(2, count);
    }

    @Test
    @DisplayName("should find notifications scheduled for retry")
    void findByStatusAndNextRetryAtBefore_ReturnsScheduledNotifications() {
        Notification notification = new Notification(
            UUID.randomUUID(), Channel.EMAIL,
            new Recipient("user@example.com", null, null, null),
            null, Map.of(), null, null);

        Notification saved = notificationRepository.save(notification);
        saved.markProcessing();
        saved.markRetryScheduled(Instant.now().minusSeconds(60));
        notificationRepository.save(saved);

        List<Notification> retryList = notificationRepository.findByStatusAndNextRetryAtBefore(
            NotificationStatus.RETRY_SCHEDULED, Instant.now(), 10);

        assertEquals(1, retryList.size());
    }
}