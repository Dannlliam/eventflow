package com.eventflow.identity.interfaces;

import com.eventflow.identity.application.CreateUserUseCase;
import com.eventflow.identity.application.GenerateApiKeyUseCase;
import com.eventflow.identity.domain.Role;
import com.eventflow.identity.domain.User;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GraphQL resolver for user management operations.
 * Provides query and mutation endpoints for the Admin Dashboard.
 */
@Controller
public class UserGraphQLResolver {

    private static final Logger log = LoggerFactory.getLogger(UserGraphQLResolver.class);

    private final CreateUserUseCase createUserUseCase;
    private final GenerateApiKeyUseCase generateApiKeyUseCase;

    public UserGraphQLResolver(CreateUserUseCase createUserUseCase,
                                GenerateApiKeyUseCase generateApiKeyUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.generateApiKeyUseCase = generateApiKeyUseCase;
    }

    @MutationMapping
    public UserPayload createUser(@Argument @NotBlank String email,
                                   @Argument @NotBlank String displayName,
                                   @Argument List<String> roles) {
        log.info("Creating user: email={}, displayName={}, roles={}", email, displayName, roles);

        Set<Role> roleSet = roles.stream()
            .map(Role::fromString)
            .collect(Collectors.toSet());

        User user = createUserUseCase.execute(email, displayName, roleSet);

        return new UserPayload(
            user.getId().toString(),
            user.getEmail().value(),
            user.getDisplayName(),
            user.getRoles().stream().map(Role::name).toList(),
            user.isEnabled(),
            user.getCreatedAt().toString()
        );
    }

    @MutationMapping
    public ApiKeyPayload generateApiKey(@Argument String description) {
        log.info("Generating API key: description={}", description);

        UUID wsId = UUID.randomUUID(); // In production, extracted from workspace context
        UUID userId = UUID.randomUUID(); // In production, extracted from security context

        GenerateApiKeyUseCase.ApiKeyResult result = generateApiKeyUseCase.execute(
            wsId, userId, description);

        return new ApiKeyPayload(
            result.id().toString(),
            result.keyPrefix(),
            result.rawKey(),
            description,
            java.time.Instant.now().toString()
        );
    }

    @MutationMapping
    public Boolean deactivateApiKey(@Argument String keyId) {
        log.info("Deactivating API key: keyId={}", keyId);
        
        // TODO: Implement actual API key deactivation
        return true;
    }

    // GraphQL DTOs

    public record UserPayload(
        String id,
        String email,
        String displayName,
        List<String> roles,
        boolean enabled,
        String createdAt
    ) {}

    public record ApiKeyPayload(
        String id,
        String keyPrefix,
        String fullKey,
        String description,
        String createdAt
    ) {}
}