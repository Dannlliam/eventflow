package com.eventflow.identity.infrastructure.persistence;

import com.eventflow.identity.domain.model.ApiKey;
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
class JpaApiKeyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaApiKeyRepositoryAdapter repository;

    @Test
    void save_shouldPersistApiKey() {
        ApiKey apiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .workspaceId(UUID.randomUUID())
                .name("Production Key")
                .keyPrefix("ef_prod")
                .keyHash("hash123")
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        ApiKey saved = repository.save(apiKey);
        entityManager.flush();

        assertThat(saved.getId()).isEqualTo(apiKey.getId());
    }

    @Test
    void findByKeyHash_shouldReturnApiKey() {
        ApiKey apiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .workspaceId(UUID.randomUUID())
                .name("Test Key")
                .keyPrefix("ef_test")
                .keyHash("unique_hash")
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        repository.save(apiKey);
        entityManager.flush();

        Optional<ApiKey> result = repository.findByKeyHash("unique_hash");

        assertThat(result).isPresent();
        assertThat(result.get().getKeyPrefix()).isEqualTo("ef_test");
    }

    @Test
    void findByUserId_shouldReturnAllKeys() {
        UUID userId = UUID.randomUUID();
        
        repository.save(ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .workspaceId(UUID.randomUUID())
                .name("Key 1")
                .keyPrefix("ef_test1")
                .keyHash("hash1")
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build());
        
        repository.save(ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .workspaceId(UUID.randomUUID())
                .name("Key 2")
                .keyPrefix("ef_test2")
                .keyHash("hash2")
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build());
        
        entityManager.flush();

        List<ApiKey> results = repository.findByUserId(userId);

        assertThat(results).hasSize(2);
    }

    @Test
    void findByWorkspaceId_shouldReturnAllKeys() {
        UUID workspaceId = UUID.randomUUID();
        
        repository.save(ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Key 1")
                .keyPrefix("ef_prod1")
                .keyHash("hash1")
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build());
        
        repository.save(ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Key 2")
                .keyPrefix("ef_prod2")
                .keyHash("hash2")
                .status(ApiKey.Status.REVOKED)
                .createdAt(Instant.now())
                .build());
        
        entityManager.flush();

        List<ApiKey> results = repository.findByWorkspaceId(workspaceId);

        assertThat(results).hasSize(2);
    }

    @Test
    void updateLastUsedAt_shouldUpdateTimestamp() {
        ApiKey apiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .workspaceId(UUID.randomUUID())
                .name("Test Key")
                .keyPrefix("ef_test")
                .keyHash("hash")
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        repository.save(apiKey);
        entityManager.flush();
        entityManager.clear();

        Instant now = Instant.now();
        repository.updateLastUsedAt(apiKey.getId(), now);
        entityManager.flush();
        entityManager.clear();

        ApiKey updated = repository.findById(apiKey.getId()).orElseThrow();
        assertThat(updated.getLastUsedAt()).isNotNull();
    }

    @Test
    void revokeApiKey_shouldUpdateStatus() {
        ApiKey apiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .workspaceId(UUID.randomUUID())
                .name("To Revoke")
                .keyPrefix("ef_test")
                .keyHash("hash")
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        repository.save(apiKey);
        entityManager.flush();
        entityManager.clear();

        ApiKey revoked = ApiKey.builder()
                .id(apiKey.getId())
                .userId(apiKey.getUserId())
                .workspaceId(apiKey.getWorkspaceId())
                .name(apiKey.getName())
                .keyPrefix(apiKey.getKeyPrefix())
                .keyHash(apiKey.getKeyHash())
                .status(ApiKey.Status.REVOKED)
                .createdAt(apiKey.getCreatedAt())
                .build();
        repository.save(revoked);
        entityManager.flush();

        ApiKey result = repository.findById(apiKey.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ApiKey.Status.REVOKED);
    }

    @Test
    void findByStatus_shouldFilterByStatus() {
        UUID workspaceId = UUID.randomUUID();
        
        repository.save(ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Active Key")
                .keyPrefix("ef_active")
                .keyHash("hash1")
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build());
        
        repository.save(ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Revoked Key")
                .keyPrefix("ef_revoked")
                .keyHash("hash2")
                .status(ApiKey.Status.REVOKED)
                .createdAt(Instant.now())
                .build());
        
        entityManager.flush();

        List<ApiKey> active = repository.findByStatus(ApiKey.Status.ACTIVE);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getStatus()).isEqualTo(ApiKey.Status.ACTIVE);
    }
}
