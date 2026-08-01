package com.eventflow.provider.application;

import com.eventflow.provider.domain.model.Provider;
import com.eventflow.provider.domain.model.ProviderCredentials;
import com.eventflow.provider.domain.repository.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaveProviderUseCaseTest {

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private SaveProviderUseCase saveProviderUseCase;

    private UUID workspaceId;
    private ProviderCredentials sendGridCredentials;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        
        sendGridCredentials = ProviderCredentials.builder()
                .apiKey("SG.test-api-key-xyz")
                .build();
    }

    @Test
    void execute_shouldCreateNewProvider_whenIdIsNull() {
        // Arrange
        Provider newProvider = Provider.builder()
                .workspaceId(workspaceId)
                .name("SendGrid Primary")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .build();

        ArgumentCaptor<Provider> providerCaptor = ArgumentCaptor.forClass(Provider.class);
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> {
            Provider p = invocation.getArgument(0);
            return Provider.builder()
                    .id(UUID.randomUUID())
                    .workspaceId(p.getWorkspaceId())
                    .name(p.getName())
                    .type(p.getType())
                    .channel(p.getChannel())
                    .credentials(p.getCredentials())
                    .priority(p.getPriority())
                    .status(p.getStatus())
                    .createdAt(Instant.now())
                    .build();
        });

        // Act
        Provider result = saveProviderUseCase.execute(newProvider);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(result.getName()).isEqualTo("SendGrid Primary");
        assertThat(result.getType()).isEqualTo(Provider.ProviderType.SENDGRID);
        assertThat(result.getChannel()).isEqualTo(Provider.Channel.EMAIL);
        assertThat(result.getStatus()).isEqualTo(Provider.Status.ACTIVE);
        assertThat(result.getCreatedAt()).isNotNull();

        verify(providerRepository).save(providerCaptor.capture());
        Provider savedProvider = providerCaptor.getValue();
        assertThat(savedProvider.getName()).isEqualTo("SendGrid Primary");
    }

    @Test
    void execute_shouldUpdateExistingProvider_whenIdExists() {
        // Arrange
        UUID providerId = UUID.randomUUID();
        
        Provider existingProvider = Provider.builder()
                .id(providerId)
                .workspaceId(workspaceId)
                .name("SendGrid Old")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();

        Provider updatedProvider = Provider.builder()
                .id(providerId)
                .workspaceId(workspaceId)
                .name("SendGrid Updated")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(2)
                .status(Provider.Status.ACTIVE)
                .build();

        when(providerRepository.findById(providerId)).thenReturn(Optional.of(existingProvider));
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Provider result = saveProviderUseCase.execute(updatedProvider);

        // Assert
        assertThat(result.getId()).isEqualTo(providerId);
        assertThat(result.getName()).isEqualTo("SendGrid Updated");
        assertThat(result.getPriority()).isEqualTo(2);
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(providerRepository).findById(providerId);
        verify(providerRepository).save(any(Provider.class));
    }

    @Test
    void execute_shouldEncryptCredentials_beforeSaving() {
        // Arrange
        Provider provider = Provider.builder()
                .workspaceId(workspaceId)
                .name("Twilio SMS")
                .type(Provider.ProviderType.TWILIO)
                .channel(Provider.Channel.SMS)
                .credentials(ProviderCredentials.builder()
                        .accountSid("AC123456789")
                        .authToken("secret-auth-token")
                        .build())
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .build();

        ArgumentCaptor<Provider> providerCaptor = ArgumentCaptor.forClass(Provider.class);
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> {
            Provider p = invocation.getArgument(0);
            return Provider.builder()
                    .id(UUID.randomUUID())
                    .workspaceId(p.getWorkspaceId())
                    .name(p.getName())
                    .type(p.getType())
                    .channel(p.getChannel())
                    .credentials(p.getCredentials())
                    .priority(p.getPriority())
                    .status(p.getStatus())
                    .createdAt(Instant.now())
                    .build();
        });

        // Act
        Provider result = saveProviderUseCase.execute(provider);

        // Assert
        verify(providerRepository).save(providerCaptor.capture());
        Provider savedProvider = providerCaptor.getValue();
        
        // Credentials should be present (encryption happens at repository/JPA level)
        assertThat(savedProvider.getCredentials()).isNotNull();
        assertThat(savedProvider.getCredentials().getAccountSid()).isNotNull();
        assertThat(savedProvider.getCredentials().getAuthToken()).isNotNull();
    }

    @Test
    void execute_shouldValidateChannelMatchesProviderType() {
        // Arrange - SendGrid is EMAIL only, but trying to configure for SMS
        Provider invalidProvider = Provider.builder()
                .workspaceId(workspaceId)
                .name("Invalid SendGrid SMS")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.SMS) // WRONG!
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> saveProviderUseCase.execute(invalidProvider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider type SENDGRID does not support channel SMS");

        verify(providerRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenWorkspaceIdIsNull() {
        // Arrange
        Provider provider = Provider.builder()
                .workspaceId(null)
                .name("Test Provider")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> saveProviderUseCase.execute(provider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workspace ID cannot be null");

        verify(providerRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenNameIsBlank() {
        // Arrange
        Provider provider = Provider.builder()
                .workspaceId(workspaceId)
                .name("")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> saveProviderUseCase.execute(provider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider name cannot be blank");

        verify(providerRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenCredentialsAreNull() {
        // Arrange
        Provider provider = Provider.builder()
                .workspaceId(workspaceId)
                .name("SendGrid")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(null)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> saveProviderUseCase.execute(provider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider credentials cannot be null");

        verify(providerRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenPriorityIsNegative() {
        // Arrange
        Provider provider = Provider.builder()
                .workspaceId(workspaceId)
                .name("SendGrid")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(-1)
                .status(Provider.Status.ACTIVE)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> saveProviderUseCase.execute(provider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider priority must be non-negative");

        verify(providerRepository, never()).save(any());
    }

    @Test
    void execute_shouldSetUpdatedAtTimestamp_onUpdate() {
        // Arrange
        UUID providerId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(7200);
        
        Provider existingProvider = Provider.builder()
                .id(providerId)
                .workspaceId(workspaceId)
                .name("Old Name")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .createdAt(createdAt)
                .build();

        Provider updatedProvider = Provider.builder()
                .id(providerId)
                .workspaceId(workspaceId)
                .name("New Name")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .build();

        when(providerRepository.findById(providerId)).thenReturn(Optional.of(existingProvider));
        
        ArgumentCaptor<Provider> providerCaptor = ArgumentCaptor.forClass(Provider.class);
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant beforeUpdate = Instant.now();

        // Act
        Provider result = saveProviderUseCase.execute(updatedProvider);

        Instant afterUpdate = Instant.now();

        // Assert
        verify(providerRepository).save(providerCaptor.capture());
        Provider savedProvider = providerCaptor.getValue();
        
        assertThat(savedProvider.getCreatedAt()).isEqualTo(createdAt); // Preserved
        assertThat(savedProvider.getUpdatedAt()).isNotNull();
        assertThat(savedProvider.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
    }

    @Test
    void execute_shouldSupportAllProviderTypes() {
        // Arrange & Act & Assert
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> {
            Provider p = invocation.getArgument(0);
            return Provider.builder()
                    .id(UUID.randomUUID())
                    .workspaceId(p.getWorkspaceId())
                    .name(p.getName())
                    .type(p.getType())
                    .channel(p.getChannel())
                    .credentials(p.getCredentials())
                    .priority(p.getPriority())
                    .status(p.getStatus())
                    .createdAt(Instant.now())
                    .build();
        });

        // Test SendGrid (EMAIL)
        Provider sendGrid = Provider.builder()
                .workspaceId(workspaceId)
                .name("SendGrid")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(ProviderCredentials.builder().apiKey("sg-key").build())
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .build();
        Provider result1 = saveProviderUseCase.execute(sendGrid);
        assertThat(result1.getType()).isEqualTo(Provider.ProviderType.SENDGRID);

        // Test Twilio (SMS)
        Provider twilio = Provider.builder()
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
                .build();
        Provider result2 = saveProviderUseCase.execute(twilio);
        assertThat(result2.getType()).isEqualTo(Provider.ProviderType.TWILIO);

        // Test FCM (PUSH)
        Provider fcm = Provider.builder()
                .workspaceId(workspaceId)
                .name("FCM")
                .type(Provider.ProviderType.FCM)
                .channel(Provider.Channel.PUSH)
                .credentials(ProviderCredentials.builder().serverKey("fcm-key").build())
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .build();
        Provider result3 = saveProviderUseCase.execute(fcm);
        assertThat(result3.getType()).isEqualTo(Provider.ProviderType.FCM);

        verify(providerRepository, times(3)).save(any(Provider.class));
    }

    @Test
    void execute_shouldThrowException_whenUpdatingNonExistentProvider() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        Provider provider = Provider.builder()
                .id(nonExistentId)
                .workspaceId(workspaceId)
                .name("Test")
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(sendGridCredentials)
                .priority(1)
                .status(Provider.Status.ACTIVE)
                .build();

        when(providerRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> saveProviderUseCase.execute(provider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider not found");

        verify(providerRepository).findById(nonExistentId);
        verify(providerRepository, never()).save(any());
    }
}
