package com.eventflow.provider.infrastructure.dispatcher.webhook;

import com.eventflow.common.infrastructure.security.SsrfProtectionService;
import com.eventflow.common.infrastructure.security.WebhookSigningService;
import com.eventflow.provider.domain.model.DispatchResult;
import com.eventflow.provider.domain.model.Provider;
import com.eventflow.provider.domain.model.ProviderCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookDispatcherTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SsrfProtectionService ssrfProtectionService;

    @Mock
    private WebhookSigningService webhookSigningService;

    private WebhookDispatcher dispatcher;
    private Provider provider;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        dispatcher = new WebhookDispatcher(restTemplate, ssrfProtectionService, webhookSigningService);
        notificationId = UUID.randomUUID();
        
        provider = Provider.builder()
                .id(UUID.randomUUID())
                .type(Provider.ProviderType.WEBHOOK)
                .channel(Provider.Channel.WEBHOOK)
                .credentials(ProviderCredentials.builder()
                        .webhookSecret("secret123")
                        .build())
                .status(Provider.Status.ACTIVE)
                .build();
    }

    @Test
    void dispatch_shouldSendWebhookSuccessfully() {
        Map<String, Object> content = Map.of("event", "notification.delivered", "id", "123");
        String url = "https://api.example.com/webhook";

        when(ssrfProtectionService.validateUrl(url)).thenReturn(true);
        when(webhookSigningService.generateSignature(any(), any())).thenReturn("signature123");
        when(restTemplate.postForObject(any(), any(), eq(String.class))).thenReturn("OK");

        DispatchResult result = dispatcher.dispatch(notificationId, provider, url, content);

        assertThat(result.isSuccess()).isTrue();
        verify(ssrfProtectionService).validateUrl(url);
        verify(webhookSigningService).generateSignature(any(), eq("secret123"));
    }

    @Test
    void dispatch_shouldBlockSsrfAttempt() {
        String maliciousUrl = "http://localhost:8080/admin";
        Map<String, Object> content = Map.of("event", "test");

        when(ssrfProtectionService.validateUrl(maliciousUrl)).thenReturn(false);

        DispatchResult result = dispatcher.dispatch(notificationId, provider, maliciousUrl, content);

        assertThat(result.isSuccess()).isFalse();
        verify(restTemplate, never()).postForObject(any(), any(), any());
    }

    @Test
    void dispatch_shouldHandleFailure() {
        String url = "https://api.example.com/webhook";
        Map<String, Object> content = Map.of("event", "test");

        when(ssrfProtectionService.validateUrl(url)).thenReturn(true);
        when(webhookSigningService.generateSignature(any(), any())).thenReturn("sig");
        when(restTemplate.postForObject(any(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection timeout"));

        DispatchResult result = dispatcher.dispatch(notificationId, provider, url, content);

        assertThat(result.isSuccess()).isFalse();
    }
}
