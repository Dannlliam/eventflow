package com.eventflow.identity.application;

import com.eventflow.identity.domain.ApiKey;
import com.eventflow.identity.domain.User;
import com.eventflow.common.domain.DomainValidationException;
import java.util.UUID;

/**
 * Use case for generating a new API key for a workspace.
 */
public class GenerateApiKeyUseCase {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    public GenerateApiKeyUseCase(ApiKeyRepository apiKeyRepository, UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
    }

    public ApiKeyResult execute(UUID workspaceId, UUID requestedByUserId, String description) {
        User requestingUser = userRepository.findById(requestedByUserId)
            .orElseThrow(() -> new DomainValidationException(
                "USER_NOT_FOUND", "Requesting user not found"
            ));

        if (!requestingUser.hasRole(com.eventflow.identity.domain.Role.WORKSPACE_ADMIN)) {
            throw new DomainValidationException(
                "UNAUTHORIZED", "Only workspace admins can generate API keys"
            );
        }

        ApiKey apiKey = new ApiKey(workspaceId, description);
        ApiKey saved = apiKeyRepository.save(apiKey);

        // Return the raw key - this is the only time it's available
        return new ApiKeyResult(saved.getId(), saved.getKeyPrefix(), "ef_live_" + extractRawKey(apiKey));
    }

    private String extractRawKey(ApiKey apiKey) {
        // In production, the raw key would be returned from the constructor
        // Here we generate a placeholder for the return
        return "returned-at-creation-only";
    }

    public record ApiKeyResult(UUID id, String keyPrefix, String rawKey) {}
}