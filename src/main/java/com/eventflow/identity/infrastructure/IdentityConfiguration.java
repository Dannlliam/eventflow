package com.eventflow.identity.infrastructure;

import com.eventflow.identity.application.ApiKeyRepository;
import com.eventflow.identity.application.CreateUserUseCase;
import com.eventflow.identity.application.GenerateApiKeyUseCase;
import com.eventflow.identity.application.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for the Identity bounded context.
 * Wires use cases with their repository implementations.
 */
@Configuration
public class IdentityConfiguration {

    @Bean
    @Primary
    public UserRepository userRepository(SpringDataUserRepository springDataRepository) {
        return new JpaUserRepository(springDataRepository);
    }

    @Bean
    @Primary
    public ApiKeyRepository apiKeyRepository(SpringDataApiKeyRepository springDataRepository) {
        return new JpaApiKeyRepository(springDataRepository);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(UserRepository userRepository) {
        return new CreateUserUseCase(userRepository);
    }

    @Bean
    public GenerateApiKeyUseCase generateApiKeyUseCase(ApiKeyRepository apiKeyRepository,
                                                        UserRepository userRepository) {
        return new GenerateApiKeyUseCase(apiKeyRepository, userRepository);
    }
}
