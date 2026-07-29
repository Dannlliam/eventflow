package com.eventflow.provider.infrastructure;

import com.eventflow.identity.application.UserRepository;
import com.eventflow.provider.application.NotificationDispatcherPort;
import com.eventflow.provider.application.ProviderRepository;
import com.eventflow.provider.application.ProviderSelectionUseCase;
import com.eventflow.provider.application.SaveProviderUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for the Provider bounded context.
 */
@Configuration
public class ProviderConfiguration {

    @Bean
    @Primary
    public ProviderRepository providerRepository(SpringDataProviderRepository springDataRepository,
                                                   ObjectMapper objectMapper) {
        return new JpaProviderRepository(springDataRepository, objectMapper);
    }

    @Bean
    public SaveProviderUseCase saveProviderUseCase(ProviderRepository providerRepository,
                                                    UserRepository userRepository) {
        return new SaveProviderUseCase(providerRepository, userRepository);
    }

    @Bean
    public ProviderSelectionUseCase providerSelectionUseCase(ProviderRepository providerRepository) {
        return new ProviderSelectionUseCase(providerRepository);
    }

    @Bean
    public NotificationDispatcherPort notificationDispatcher(
            SendGridEmailDispatcher sendGrid,
            TwilioSmsDispatcher twilio,
            FcmPushDispatcher fcm,
            WebhookDispatcher webhook) {
        return new CompositeNotificationDispatcher(sendGrid, twilio, fcm, webhook);
    }
}
