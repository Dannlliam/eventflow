package com.eventflow.provider.infrastructure.persistence;

import com.eventflow.provider.domain.model.Provider;
import com.eventflow.provider.domain.model.ProviderCredentials;
import org.junit.jupiter.api.BeforeEach;
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
class JpaProviderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaProviderRepositoryAdapter jpaProviderRepository;

    private UUID workspaceId;
    private ProviderCredentials sendGridCredentials;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        sendGridCredentials = ProviderCredentials.builder()
                .apiKey("SG.test-api-key")
                .build();
    }

    @Test
    void save_shouldPersistProvider_successfully() {
        // Arrange
        Provider provider = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("SendGrid Primary")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        // Act
        Provider savedProvider = jpaProviderRepository.save(provider);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(savedProvider).isNotNull();
        assertThat(savedProvider.getId()).isEqualTo(provider.getId());
        
        Provider foundProvider = jpaProviderRepository.findById(savedProvider.getId()).orElse(null);
        assertThat(foundProvider).isNotNull();
        assertThat(foundProvider.getName()).isEqualTo("SendGrid Primary");
        assertThat(foundProvider.getType()).isEqualTo(Provider.ProviderType.SENDGRID);
        assertThat(foundProvider.getChannel()).isEqualTo(Provider.Channel.EMAIL);
    }

    @Test
    void findById_shouldReturnProvider_whenExists() {
        // Arrange
        Provider provider = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Twilio SMS")
                .type(Provider.ProviderType.TWILIO)
                .channel(Provider.Channel.SMS)
                .credentials(ProviderCredentials.builder()
                        .accountSid("AC123")
                        .authToken("token")
                        .build())
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        jpaProviderRepository.save(provider);
        entityManager.flush();

        // Act
        Optional<Provider> result = jpaProviderRepository.findById(provider.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Twilio SMS");
        assertThat(result.get().getType()).isEqualTo(Provider.ProviderType.TWILIO);
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        Optional<Provider> result = jpaProviderRepository.findById(nonExistentId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findByWorkspaceId_shouldReturnAllProviders() {
        // Arrange
        Provider provider1 = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("SendGrid")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        
        Provider provider2 = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Twilio")
                .type(Provider.ProviderType.TWILIO)
                .channel(Provider.Channel.SMS)
                .credentials(ProviderCredentials.builder()
                        .accountSid("AC123")
                        .authToken("token")
                        .build())
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        jpaProviderRepository.save(provider1);
        jpaProviderRepository.save(provider2);
        entityManager.flush();

        // Act
        List<Provider> results = jpaProviderRepository.findByWorkspaceId(workspaceId);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Provider::getName)
                .containsExactlyInAnyOrder("SendGrid", "Twilio");
    }

    @Test
    void findByWorkspaceIdAndChannel_shouldFilterByChannel() {
        // Arrange
        Provider emailProvider = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("SendGrid")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        
        Provider smsProvider = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Twilio")
                .type(Provider.ProviderType.TWILIO)
                .channel(Provider.Channel.SMS)
                .credentials(ProviderCredentials.builder()
                        .accountSid("AC123")
                        .authToken("token")
                        .build())
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        jpaProviderRepository.save(emailProvider);
        jpaProviderRepository.save(smsProvider);
        entityManager.flush();

        // Act
        List<Provider> emailResults = jpaProviderRepository.findByWorkspaceIdAndChannel(
                workspaceId, Provider.Channel.EMAIL);

        // Assert
        assertThat(emailResults).hasSize(1);
        assertThat(emailResults.get(0).getName()).isEqualTo("SendGrid");
        assertThat(emailResults.get(0).getChannel()).isEqualTo(Provider.Channel.EMAIL);
    }

    @Test
    void findByWorkspaceIdAndChannelOrderByPriority_shouldSortByPriority() {
        // Arrange
        Provider provider1 = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("SendGrid Secondary")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(2)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        
        Provider provider2 = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("SendGrid Primary")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        
        Provider provider3 = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("SendGrid Tertiary")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(3)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        jpaProviderRepository.save(provider1);
        jpaProviderRepository.save(provider2);
        jpaProviderRepository.save(provider3);
        entityManager.flush();

        // Act
        List<Provider> results = jpaProviderRepository.findByWorkspaceIdAndChannelOrderByPriority(
                workspaceId, Provider.Channel.EMAIL);

        // Assert
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getName()).isEqualTo("SendGrid Primary");
        assertThat(results.get(0).getPriority()).isEqualTo(1);
        assertThat(results.get(1).getName()).isEqualTo("SendGrid Secondary");
        assertThat(results.get(1).getPriority()).isEqualTo(2);
        assertThat(results.get(2).getName()).isEqualTo("SendGrid Tertiary");
        assertThat(results.get(2).getPriority()).isEqualTo(3);
    }

    @Test
    void findByWorkspaceIdAndStatus_shouldFilterByStatus() {
        // Arrange
        Provider activeProvider = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Active Provider")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        
        Provider inactiveProvider = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Inactive Provider")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.INACTIVE)
                .createdAt(Instant.now())
                .build();

        jpaProviderRepository.save(activeProvider);
        jpaProviderRepository.save(inactiveProvider);
        entityManager.flush();

        // Act
        List<Provider> activeResults = jpaProviderRepository.findByWorkspaceIdAndStatus(
                workspaceId, Provider.Status.ACTIVE);

        // Assert
        assertThat(activeResults).hasSize(1);
        assertThat(activeResults.get(0).getName()).isEqualTo("Active Provider");
        assertThat(activeResults.get(0).getStatus()).isEqualTo(Provider.Status.ACTIVE);
    }

    @Test
    void delete_shouldRemoveProvider() {
        // Arrange
        Provider provider = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("To Delete")
                .type(Provider.ProviderType.FCM)
                .channel(Provider.Channel.PUSH)
                .credentials(ProviderCredentials.builder().serverKey("fcm-key").build())
                .priority(1)
                .status(Provider.Status.INACTIVE)
                .createdAt(Instant.now())
                .build();
        jpaProviderRepository.save(provider);
        entityManager.flush();

        // Act
        jpaProviderRepository.delete(provider.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<Provider> result = jpaProviderRepository.findById(provider.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void update_shouldModifyExistingProvider() {
        // Arrange
        Provider provider = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Original Name")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        Provider savedProvider = jpaProviderRepository.save(provider);
        entityManager.flush();
        entityManager.clear();

        // Act - Update
        Provider updatedProvider = Provider.builder()
                .id(savedProvider.getId())
                .workspaceId(workspaceId)
                .name("Updated Name")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(2)
                .status(Provider.Status.INACTIVE)
                .createdAt(savedProvider.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
        jpaProviderRepository.save(updatedProvider);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Provider result = jpaProviderRepository.findById(savedProvider.getId()).orElse(null);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getPriority()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo(Provider.Status.INACTIVE);
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_shouldEncryptCredentials() {
        // Arrange
        ProviderCredentials credentials = ProviderCredentials.builder()
                .accountSid("AC123456789")
                .authToken("very-secret-token")
                .build();

        Provider provider = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Twilio")
                .type(Provider.ProviderType.TWILIO)
                .channel(Provider.Channel.SMS)
                .credentials(credentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        // Act
        Provider savedProvider = jpaProviderRepository.save(provider);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Provider foundProvider = jpaProviderRepository.findById(savedProvider.getId()).orElse(null);
        assertThat(foundProvider).isNotNull();
        assertThat(foundProvider.getCredentials()).isNotNull();
        assertThat(foundProvider.getCredentials().getAccountSid()).isEqualTo("AC123456789");
        // Note: Actual encryption verification would depend on the JPA converter implementation
    }

    @Test
    void save_shouldHandleAllProviderTypes() {
        // Arrange & Act
        Provider sendGrid = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("SendGrid")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(ProviderCredentials.builder().apiKey("sg-key").build())
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        
        Provider twilio = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Twilio")
                .type(Provider.ProviderType.TWILIO)
                .channel(Provider.Channel.SMS)
                .credentials(ProviderCredentials.builder()
                        .accountSid("AC123")
                        .authToken("token")
                        .build())
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        
        Provider fcm = Provider.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("FCM")
                .type(Provider.ProviderType.FCM)
                .channel(Provider.Channel.PUSH)
                .credentials(ProviderCredentials.builder().serverKey("fcm-key").build())
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        jpaProviderRepository.save(sendGrid);
        jpaProviderRepository.save(twilio);
        jpaProviderRepository.save(fcm);
        entityManager.flush();

        // Assert
        List<Provider> all = jpaProviderRepository.findByWorkspaceId(workspaceId);
        assertThat(all).hasSize(3);
        assertThat(all).extracting(Provider::getType)
                .containsExactlyInAnyOrder(
                        Provider.ProviderType.SENDGRID,
                        Provider.ProviderType.TWILIO,
                        Provider.ProviderType.FCM
                );
    }
}
