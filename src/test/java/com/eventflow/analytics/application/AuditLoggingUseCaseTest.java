package com.eventflow.analytics.application;

import com.eventflow.analytics.domain.model.AuditLog;
import com.eventflow.analytics.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLoggingUseCaseTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLoggingUseCase auditLoggingUseCase;

    private UUID workspaceId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void execute_shouldLogTemplateCreation() {
        UUID templateId = UUID.randomUUID();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        auditLoggingUseCase.logAction(
                workspaceId,
                userId,
                "TEMPLATE_CREATED",
                "Template",
                templateId,
                null,
                Map.of("name", "Welcome Email"),
                "192.168.1.1"
        );

        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        
        assertThat(log.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(log.getUserId()).isEqualTo(userId);
        assertThat(log.getAction()).isEqualTo("TEMPLATE_CREATED");
        assertThat(log.getEntityType()).isEqualTo("Template");
        assertThat(log.getEntityId()).isEqualTo(templateId);
    }

    @Test
    void execute_shouldLogTemplateUpdate() {
        UUID templateId = UUID.randomUUID();
        Map<String, Object> oldData = Map.of("name", "Old Name", "status", "DRAFT");
        Map<String, Object> newData = Map.of("name", "New Name", "status", "PUBLISHED");
        
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        auditLoggingUseCase.logAction(
                workspaceId,
                userId,
                "TEMPLATE_UPDATED",
                "Template",
                templateId,
                oldData,
                newData,
                "192.168.1.1"
        );

        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        
        assertThat(log.getAction()).isEqualTo("TEMPLATE_UPDATED");
        assertThat(log.getOldData()).isEqualTo(oldData);
        assertThat(log.getNewData()).isEqualTo(newData);
    }

    @Test
    void execute_shouldLogUserInvitation() {
        UUID newUserId = UUID.randomUUID();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        auditLoggingUseCase.logAction(
                workspaceId,
                userId,
                "USER_INVITED",
                "User",
                newUserId,
                null,
                Map.of("email", "new@example.com", "role", "DEVELOPER"),
                "192.168.1.1"
        );

        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        
        assertThat(log.getAction()).isEqualTo("USER_INVITED");
        assertThat(log.getEntityType()).isEqualTo("User");
    }

    @Test
    void execute_shouldLogProviderConfiguration() {
        UUID providerId = UUID.randomUUID();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        auditLoggingUseCase.logAction(
                workspaceId,
                userId,
                "PROVIDER_CONFIGURED",
                "Provider",
                providerId,
                null,
                Map.of("type", "SENDGRID", "channel", "EMAIL"),
                "192.168.1.1"
        );

        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        
        assertThat(log.getAction()).isEqualTo("PROVIDER_CONFIGURED");
        assertThat(log.getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    void execute_shouldLogDlqReplay() {
        UUID eventId = UUID.randomUUID();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        auditLoggingUseCase.logAction(
                workspaceId,
                userId,
                "DLQ_REPLAYED",
                "DlqEvent",
                eventId,
                null,
                Map.of("topic", "notification.created"),
                "192.168.1.1"
        );

        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        
        assertThat(log.getAction()).isEqualTo("DLQ_REPLAYED");
    }

    @Test
    void execute_shouldIncludeTimestamp() {
        UUID entityId = UUID.randomUUID();
        Instant before = Instant.now();
        
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        auditLoggingUseCase.logAction(
                workspaceId,
                userId,
                "TEST_ACTION",
                "Test",
                entityId,
                null,
                Map.of(),
                "192.168.1.1"
        );

        Instant after = Instant.now();

        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        
        assertThat(log.getTimestamp()).isBetween(before, after);
    }
}
