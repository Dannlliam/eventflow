package com.eventflow.identity.application;

import com.eventflow.identity.domain.model.ApiKey;
import com.eventflow.identity.domain.model.User;
import com.eventflow.identity.domain.repository.ApiKeyRepository;
import com.eventflow.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateApiKeyUseCaseTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GenerateApiKeyUseCase generateApiKeyUseCase;

    private UUID userId;
    private UUID workspaceId;
    private User mockUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        
        mockUser = User.builder()
                .id(userId)
                .workspaceId(workspaceId)
                .email("admin@example.com")
                .passwordHash("hashed-password")
                .role(User.Role.ADMIN)
                .status(User.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void execute_shouldGenerateApiKey_whenUserExists() {
        // Arrange
        String keyName = "Production API Key";
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        
        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ApiKey result = generateApiKeyUseCase.execute(userId, workspaceId, keyName);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(result.getName()).isEqualTo(keyName);
        assertThat(result.getKeyPrefix()).isNotNull();
        assertThat(result.getKeyHash()).isNotNull();
        assertThat(result.getPlainKey()).isNotNull();
        assertThat(result.getPlainKey()).startsWith(result.getKeyPrefix());
        assertThat(result.getStatus()).isEqualTo(ApiKey.Status.ACTIVE);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getLastUsedAt()).isNull();

        verify(userRepository).findById(userId);
        verify(apiKeyRepository).save(apiKeyCaptor.capture());
        
        ApiKey capturedKey = apiKeyCaptor.getValue();
        assertThat(capturedKey.getName()).isEqualTo(keyName);
        assertThat(capturedKey.getUserId()).isEqualTo(userId);
    }

    @Test
    void execute_shouldGenerateUniqueKey_onEachInvocation() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ApiKey key1 = generateApiKeyUseCase.execute(userId, workspaceId, "Key 1");
        ApiKey key2 = generateApiKeyUseCase.execute(userId, workspaceId, "Key 2");

        // Assert
        assertThat(key1.getPlainKey()).isNotEqualTo(key2.getPlainKey());
        assertThat(key1.getKeyPrefix()).isNotEqualTo(key2.getKeyPrefix());
        assertThat(key1.getKeyHash()).isNotEqualTo(key2.getKeyHash());
        
        verify(apiKeyRepository, times(2)).save(any(ApiKey.class));
    }

    @Test
    void execute_shouldHashApiKey_notStorePlaintext() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        
        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ApiKey result = generateApiKeyUseCase.execute(userId, workspaceId, "Test Key");

        // Assert
        verify(apiKeyRepository).save(apiKeyCaptor.capture());
        ApiKey savedKey = apiKeyCaptor.getValue();
        
        // Verify the plain key is not stored in the hash
        assertThat(savedKey.getKeyHash()).isNotEqualTo(result.getPlainKey());
        assertThat(savedKey.getKeyHash()).doesNotContain(result.getPlainKey());
        
        // Verify hash is a proper SHA-256 hash (64 hex characters)
        assertThat(savedKey.getKeyHash()).matches("^[a-f0-9]{64}$");
    }

    @Test
    void execute_shouldGenerateKeyWithPrefix() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ApiKey result = generateApiKeyUseCase.execute(userId, workspaceId, "Test Key");

        // Assert
        assertThat(result.getKeyPrefix()).hasSize(8);
        assertThat(result.getPlainKey()).startsWith(result.getKeyPrefix() + "_");
        assertThat(result.getPlainKey()).hasSizeGreaterThan(40); // Prefix + separator + random bytes
    }

    @Test
    void execute_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> generateApiKeyUseCase.execute(userId, workspaceId, "Test Key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(userId);
        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenWorkspaceMismatch() {
        // Arrange
        UUID differentWorkspaceId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // Act & Assert
        assertThatThrownBy(() -> generateApiKeyUseCase.execute(userId, differentWorkspaceId, "Test Key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workspace mismatch");

        verify(userRepository).findById(userId);
        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenUserIsNotActive() {
        // Arrange
        User suspendedUser = User.builder()
                .id(userId)
                .workspaceId(workspaceId)
                .email("suspended@example.com")
                .passwordHash("hashed-password")
                .role(User.Role.ADMIN)
                .status(User.Status.SUSPENDED)
                .createdAt(Instant.now())
                .build();
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));

        // Act & Assert
        assertThatThrownBy(() -> generateApiKeyUseCase.execute(userId, workspaceId, "Test Key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User is not active");

        verify(userRepository).findById(userId);
        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenKeyNameIsBlank() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // Act & Assert
        assertThatThrownBy(() -> generateApiKeyUseCase.execute(userId, workspaceId, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key name cannot be blank");

        assertThatThrownBy(() -> generateApiKeyUseCase.execute(userId, workspaceId, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key name cannot be blank");

        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    void execute_shouldTrimKeyName() {
        // Arrange
        String keyName = "  Production Key  ";
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ApiKey result = generateApiKeyUseCase.execute(userId, workspaceId, keyName);

        // Assert
        assertThat(result.getName()).isEqualTo("Production Key");
    }

    @Test
    void execute_shouldSetCreatedAtTimestamp() {
        // Arrange
        Instant before = Instant.now();
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ApiKey result = generateApiKeyUseCase.execute(userId, workspaceId, "Test Key");
        Instant after = Instant.now();

        // Assert
        assertThat(result.getCreatedAt()).isBetween(before, after);
        assertThat(result.getLastUsedAt()).isNull();
    }

    @Test
    void execute_shouldGenerateCryptographicallySecureKey() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act - Generate multiple keys and check for patterns
        ApiKey key1 = generateApiKeyUseCase.execute(userId, workspaceId, "Key 1");
        ApiKey key2 = generateApiKeyUseCase.execute(userId, workspaceId, "Key 2");
        ApiKey key3 = generateApiKeyUseCase.execute(userId, workspaceId, "Key 3");

        // Assert - Keys should be unique and unpredictable
        assertThat(key1.getPlainKey()).isNotEqualTo(key2.getPlainKey());
        assertThat(key2.getPlainKey()).isNotEqualTo(key3.getPlainKey());
        assertThat(key1.getPlainKey()).isNotEqualTo(key3.getPlainKey());
        
        // Each key should have sufficient entropy (at least 32 bytes of randomness)
        String keyBody1 = key1.getPlainKey().substring(key1.getPlainKey().indexOf('_') + 1);
        String keyBody2 = key2.getPlainKey().substring(key2.getPlainKey().indexOf('_') + 1);
        String keyBody3 = key3.getPlainKey().substring(key3.getPlainKey().indexOf('_') + 1);
        
        assertThat(keyBody1).hasSizeGreaterThanOrEqualTo(32);
        assertThat(keyBody2).hasSizeGreaterThanOrEqualTo(32);
        assertThat(keyBody3).hasSizeGreaterThanOrEqualTo(32);
    }
}
