package com.eventflow.provider.application;

import com.eventflow.common.domain.Channel;
import com.eventflow.common.domain.DomainValidationException;
import com.eventflow.provider.domain.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case for selecting the optimal provider for notification dispatch.
 * Implements the Provider Failover strategy:
 * - Primary provider is used first
 * - If primary fails with transient error, secondary provider is used
 * - If all providers fail, the notification is routed to retry/DLQ
 *
 * As specified in the PRD Section 54 - Provider Abstraction Layer / Failover Strategy.
 */
public class ProviderSelectionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProviderSelectionUseCase.class);

    private final ProviderRepository providerRepository;

    public ProviderSelectionUseCase(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    /**
     * Selects the primary provider for a workspace and channel.
     *
     * @param workspaceId the workspace UUID
     * @param channel the notification channel
     * @return the primary provider
     * @throws DomainValidationException if no provider is configured
     */
    public Provider selectPrimary(UUID workspaceId, Channel channel) {
        Optional<Provider> primary = providerRepository.findPrimaryByWorkspaceIdAndChannel(workspaceId, channel);

        if (primary.isEmpty()) {
            // No primary configured, try any enabled provider
            Optional<Provider> fallback = providerRepository.findFirstByWorkspaceIdAndChannelAndEnabled(workspaceId, channel);
            if (fallback.isPresent()) {
                log.info("No primary provider for workspace={}, channel={}. Using fallback: {}",
                    workspaceId, channel, fallback.get().getName());
                return fallback.get();
            }

            throw new DomainValidationException(
                "NO_PROVIDER_AVAILABLE",
                "No active provider found for workspace " + workspaceId + " and channel " + channel
            );
        }

        if (!primary.get().isEnabled()) {
            log.warn("Primary provider {} is disabled. Searching for fallback.", primary.get().getName());
            Optional<Provider> fallback = providerRepository.findFirstByWorkspaceIdAndChannelAndEnabled(workspaceId, channel);
            if (fallback.isPresent()) {
                return fallback.get();
            }
            throw new DomainValidationException(
                "PRIMARY_PROVIDER_DISABLED",
                "Primary provider is disabled and no fallback available for workspace " + workspaceId
            );
        }

        return primary.get();
    }

    /**
     * Selects a fallback provider when the primary provider fails.
     *
     * @param workspaceId the workspace UUID
     * @param channel the notification channel
     * @param excludeProviderId the provider ID to exclude (the failed primary)
     * @return the fallback provider, or empty if none available
     */
    public Optional<Provider> selectFallback(UUID workspaceId, Channel channel, UUID excludeProviderId) {
        List<Provider> providers = providerRepository.findByWorkspaceIdAndChannel(workspaceId, channel);

        return providers.stream()
            .filter(p -> !p.getId().equals(excludeProviderId))
            .filter(Provider::isEnabled)
            .findFirst();
    }

    /**
     * Gets all available providers for a workspace and channel, ordered by priority.
     *
     * @param workspaceId the workspace UUID
     * @param channel the notification channel
     * @return ordered list of available providers
     */
    public List<Provider> getAvailableProviders(UUID workspaceId, Channel channel) {
        List<Provider> providers = providerRepository.findByWorkspaceIdAndChannel(workspaceId, channel);

        return providers.stream()
            .filter(Provider::isEnabled)
            .sorted((a, b) -> {
                if (a.isPrimary() && !b.isPrimary()) return -1;
                if (!a.isPrimary() && b.isPrimary()) return 1;
                return 0;
            })
            .toList();
    }

    /**
     * Checks if a workspace has at least one enabled provider for a channel.
     *
     * @param workspaceId the workspace UUID
     * @param channel the notification channel
     * @return true if at least one provider is available
     */
    public boolean hasAvailableProvider(UUID workspaceId, Channel channel) {
        return providerRepository.findFirstByWorkspaceIdAndChannelAndEnabled(workspaceId, channel).isPresent();
    }
}