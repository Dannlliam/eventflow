package com.eventflow.identity.application;

import com.eventflow.identity.domain.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for API Key persistence operations.
 */
public interface ApiKeyRepository {
    ApiKey save(ApiKey apiKey);
    Optional<ApiKey> findById(UUID id);
    Optional<ApiKey> findByKeyHash(String keyHash);
    List<ApiKey> findByWorkspaceId(UUID workspaceId);
    void deactivate(UUID id);
}