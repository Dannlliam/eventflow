package com.eventflow.identity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the api_keys table.
 */
@Repository
public interface SpringDataApiKeyRepository extends JpaRepository<ApiKeyJpaEntity, UUID> {
    Optional<ApiKeyJpaEntity> findByKeyHash(String keyHash);
    List<ApiKeyJpaEntity> findByWorkspaceId(UUID workspaceId);
}