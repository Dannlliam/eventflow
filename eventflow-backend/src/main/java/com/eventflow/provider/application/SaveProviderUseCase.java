package com.eventflow.provider.application;

import com.eventflow.common.domain.DomainValidationException;
import com.eventflow.identity.domain.User;
import com.eventflow.identity.application.UserRepository;
import com.eventflow.provider.domain.Provider;
import com.eventflow.provider.domain.ProviderType;
import com.eventflow.common.domain.Channel;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for saving/updating provider configuration.
 */
public class SaveProviderUseCase {

    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;

    public SaveProviderUseCase(ProviderRepository providerRepository, UserRepository userRepository) {
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
    }

    public Provider execute(SaveProviderCommand command) {
        User requestingUser = userRepository.findById(command.requestedByUserId())
            .orElseThrow(() -> new DomainValidationException(
                "USER_NOT_FOUND", "Requesting user not found"
            ));

        if (!requestingUser.hasRole(com.eventflow.identity.domain.Role.WORKSPACE_ADMIN)) {
            throw new DomainValidationException(
                "UNAUTHORIZED", "Only workspace admins can manage provider configurations"
            );
        }

        Channel channel = Channel.fromString(command.channel());
        ProviderType providerType = ProviderType.fromString(command.providerType());

        Provider provider = new Provider(
            command.workspaceId(),
            command.name(),
            providerType,
            channel,
            command.isPrimary(),
            command.rateLimit(),
            command.rateLimitDurationSeconds(),
            command.credentials(),
            command.settings()
        );

        return providerRepository.save(provider);
    }

    public record SaveProviderCommand(
        UUID workspaceId,
        UUID requestedByUserId,
        String name,
        String providerType,
        String channel,
        boolean isPrimary,
        int rateLimit,
        int rateLimitDurationSeconds,
        Map<String, String> credentials,
        Map<String, String> settings
    ) {}
}