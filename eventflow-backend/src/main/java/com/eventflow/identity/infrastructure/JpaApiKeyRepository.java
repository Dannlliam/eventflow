package com.eventflow.identity.infrastructure;

import com.eventflow.identity.application.ApiKeyRepository;
import com.eventflow.identity.domain.ApiKey;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of the ApiKeyRepository port.
 */
@Repository
@Transactional
public class JpaApiKeyRepository implements ApiKeyRepository {

    private final SpringDataApiKeyRepository springDataRepository;

    public JpaApiKeyRepository(SpringDataApiKeyRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public ApiKey save(ApiKey apiKey) {
        ApiKeyJpaEntity entity = toJpaEntity(apiKey);
        ApiKeyJpaEntity saved = springDataRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<ApiKey> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public Optional<ApiKey> findByKeyHash(String keyHash) {
        return springDataRepository.findByKeyHash(keyHash).map(this::toDomainEntity);
    }

    @Override
    public List<ApiKey> findByWorkspaceId(UUID workspaceId) {
        return springDataRepository.findByWorkspaceId(workspaceId).stream()
            .map(this::toDomainEntity)
            .toList();
    }

    @Override
    public void deactivate(UUID id) {
        springDataRepository.findById(id).ifPresent(entity -> {
            entity.setActive(false);
            springDataRepository.save(entity);
        });
    }

    private ApiKeyJpaEntity toJpaEntity(ApiKey domain) {
        ApiKeyJpaEntity entity = new ApiKeyJpaEntity();
        entity.setId(domain.getId());
        entity.setWorkspaceId(domain.getWorkspaceId());
        entity.setKeyPrefix(domain.getKeyPrefix());
        entity.setKeyHash(domain.getKeyHash());
        entity.setDescription(domain.getDescription());
        entity.setActive(domain.isActive());
        entity.setLastUsedAt(domain.getLastUsedAt().orElse(null));
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
        return entity;
    }

    private ApiKey toDomainEntity(ApiKeyJpaEntity entity) {
        return new ApiKey(
            entity.getId(),
            entity.getWorkspaceId(),
            entity.getKeyPrefix(),
            entity.getKeyHash(),
            entity.getDescription(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion(),
            entity.getLastUsedAt()
        );
    }
}