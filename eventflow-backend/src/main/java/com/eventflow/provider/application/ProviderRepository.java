package com.eventflow.provider.application;

import com.eventflow.provider.domain.Provider;
import com.eventflow.provider.domain.ProviderType;
import com.eventflow.common.domain.Channel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for Provider configuration persistence operations.
 */
public interface ProviderRepository {
    Provider save(Provider provider);
    Optional<Provider> findById(UUID id);
    Optional<Provider> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    List<Provider> findByWorkspaceIdAndChannel(UUID workspaceId, Channel channel);
    List<Provider> findByWorkspaceId(UUID workspaceId);
    Optional<Provider> findPrimaryByWorkspaceIdAndChannel(UUID workspaceId, Channel channel);

    /**
     * Finds the first enabled provider for a workspace and channel.
     * Used as fallback when primary provider is unavailable.
     */
    Optional<Provider> findFirstByWorkspaceIdAndChannelAndEnabled(UUID workspaceId, Channel channel);

    /**
     * Deletes a provider by its ID.
     */
    void deleteById(UUID id);
}