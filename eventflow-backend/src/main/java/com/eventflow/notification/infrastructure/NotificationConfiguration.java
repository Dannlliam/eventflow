package com.eventflow.notification.infrastructure;

import com.eventflow.common.infrastructure.EventFlowProperties;
import com.eventflow.common.infrastructure.ObservabilityConfig;
import com.eventflow.common.infrastructure.PhoneNumberNormalizationService;
import com.eventflow.notification.application.*;
import com.eventflow.provider.application.ProviderRepository;
import com.eventflow.template.application.TemplateRendererPort;
import com.eventflow.template.application.TemplateRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for the Notification bounded context.
 * Wires use cases with their repository implementations.
 */
@Configuration
public class NotificationConfiguration {

    @Bean
    @Primary
    public NotificationRepository notificationRepository(
            SpringDataNotificationRepository springDataRepository,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new JpaNotificationRepository(springDataRepository, objectMapper);
    }

    @Bean
    @Primary
    public NotificationEventRepository notificationEventRepository(
            SpringDataNotificationEventRepository springDataRepository) {
        return new JpaNotificationEventRepository(springDataRepository);
    }

    @Bean
    public IngestNotificationUseCase ingestNotificationUseCase(
            NotificationRepository notificationRepository,
            NotificationEventRepository eventRepository,
            NotificationEventPublisher eventPublisher,
            ObservabilityConfig.NotificationMetrics metrics) {
        return new IngestNotificationUseCase(notificationRepository, eventRepository, eventPublisher, metrics);
    }

    @Bean
    public ProcessNotificationUseCase processNotificationUseCase(
            NotificationRepository notificationRepository,
            NotificationEventRepository eventRepository,
            NotificationEventPublisher eventPublisher,
            TemplateRepository templateRepository,
            TemplateRendererPort templateRenderer,
            ProviderRepository providerRepository,
            PhoneNumberNormalizationService phoneNumberNormalization) {
        return new ProcessNotificationUseCase(
            notificationRepository, eventRepository, eventPublisher,
            templateRepository, templateRenderer, providerRepository,
            phoneNumberNormalization);
    }

    @Bean
    public HandleDispatchResultUseCase handleDispatchResultUseCase(
            NotificationRepository notificationRepository,
            NotificationEventRepository eventRepository,
            NotificationEventPublisher eventPublisher,
            EventFlowProperties eventFlowProperties) {
        return new HandleDispatchResultUseCase(
            notificationRepository, eventRepository, eventPublisher, eventFlowProperties);
    }

    @Bean
    public DlqReplayUseCase dlqReplayUseCase(
            NotificationRepository notificationRepository,
            NotificationEventPublisher eventPublisher) {
        return new DlqReplayUseCase(notificationRepository, eventPublisher);
    }
}
