package com.eventflow.provider.application;

import com.eventflow.common.domain.Channel;
import com.eventflow.provider.domain.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Test for Provider Selection Use Case
 * Verifies provider selection logic with priority and fallback
 */
@ExtendWith(MockitoExtension.class)
class ProviderSelectionUseCaseTest {

    @Mock
    private ProviderRepository providerRepository;

    private ProviderSelectionUseCase useCase;

    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        useCase = new ProviderSelectionUseCase(providerRepository);
        workspaceId = UUID.randomUUID();
    }

    @Test
    void shouldSelectHighestPriorityProvider() {
        // Given
        Provider provider1 = createProvider("SendGrid", 2, true);
        Provider provider2 = createProvider("Amazon SES", 1, true);
        Provider provider3 = createProvider("Mailgun", 3, true);

        when(providerRepository.findByWorkspaceIdAndChannel(workspaceId, Channel.EMAIL))
            .thenReturn(Arrays.asList(provider1, provider2, provider3));

        // When
        Provider selected = useCase.selectProvider(workspaceId, Channel.EMAIL);

        // Then
        assertThat(selected.getName()).isEqualTo("Amazon SES");
        assertThat(selected.getPriority()).isEqualTo(1);
    }

    @Test
    void shouldSkipDisabledProviders() {
        // Given
        Provider provider1 = createProvider("SendGrid", 1, false);
        Provider provider2 = createProvider("Amazon SES", 2, true);

        when(providerRepository.findByWorkspaceIdAndChannel(workspaceId, Channel.EMAIL))
            .thenReturn(Arrays.asList(provider1, provider2));

        // When
        Provider selected = useCase.selectProvider(workspaceId, Channel.EMAIL);

        // Then
        assertThat(selected.getName()).isEqualTo("Amazon SES");
    }

    @Test
    void shouldThrowExceptionWhenNoProvidersAvailable() {
        // Given
        when(providerRepository.findByWorkspaceIdAndChannel(workspaceId, Channel.EMAIL))
            .thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> useCase.selectProvider(workspaceId, Channel.EMAIL))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No providers available");
    }

    @Test
    void shouldThrowExceptionWhenAllProvidersDisabled() {
        // Given
        Provider provider1 = createProvider("SendGrid", 1, false);
        Provider provider2 = createProvider("Amazon SES", 2, false);

        when(providerRepository.findByWorkspaceIdAndChannel(workspaceId, Channel.EMAIL))
            .thenReturn(Arrays.asList(provider1, provider2));

        // When/Then
        assertThatThrownBy(() -> useCase.selectProvider(workspaceId, Channel.EMAIL))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldSupportSmsProviders() {
        // Given
        Provider twilioProvider = createSmsProvider("Twilio", 1, true);
        Provider snsProvider = createSmsProvider("Amazon SNS", 2, true);

        when(providerRepository.findByWorkspaceIdAndChannel(workspaceId, Channel.SMS))
            .thenReturn(Arrays.asList(twilioProvider, snsProvider));

        // When
        Provider selected = useCase.selectProvider(workspaceId, Channel.SMS);

        // Then
        assertThat(selected.getName()).isEqualTo("Twilio");
        assertThat(selected.getChannel()).isEqualTo(Channel.SMS);
    }

    @Test
    void shouldSupportPushProviders() {
        // Given
        Provider fcmProvider = createPushProvider("FCM", 1, true);

        when(providerRepository.findByWorkspaceIdAndChannel(workspaceId, Channel.PUSH))
            .thenReturn(List.of(fcmProvider));

        // When
        Provider selected = useCase.selectProvider(workspaceId, Channel.PUSH);

        // Then
        assertThat(selected.getName()).isEqualTo("FCM");
        assertThat(selected.getChannel()).isEqualTo(Channel.PUSH);
    }

    @Test
    void shouldHandleFailoverScenario() {
        // Given - Primary provider disabled, should fall back to secondary
        Provider primary = createProvider("SendGrid", 1, false);
        Provider secondary = createProvider("Amazon SES", 2, true);
        Provider tertiary = createProvider("Mailgun", 3, true);

        when(providerRepository.findByWorkspaceIdAndChannel(workspaceId, Channel.EMAIL))
            .thenReturn(Arrays.asList(primary, secondary, tertiary));

        // When
        Provider selected = useCase.selectProvider(workspaceId, Channel.EMAIL);

        // Then
        assertThat(selected.getName()).isEqualTo("Amazon SES");
        assertThat(selected.getPriority()).isEqualTo(2);
    }

    // Helper methods
    private Provider createProvider(String name, int priority, boolean enabled) {
        return new Provider(
            workspaceId,
            Channel.EMAIL,
            "SENDGRID",
            name,
            priority,
            Map.of("apiKey", "test-key"),
            1000,
            enabled
        );
    }

    private Provider createSmsProvider(String name, int priority, boolean enabled) {
        return new Provider(
            workspaceId,
            Channel.SMS,
            "TWILIO",
            name,
            priority,
            Map.of("accountSid", "test-sid", "authToken", "test-token"),
            100,
            enabled
        );
    }

    private Provider createPushProvider(String name, int priority, boolean enabled) {
        return new Provider(
            workspaceId,
            Channel.PUSH,
            "FCM",
            name,
            priority,
            Map.of("serverKey", "test-server-key"),
            1000,
            enabled
        );
    }
}
