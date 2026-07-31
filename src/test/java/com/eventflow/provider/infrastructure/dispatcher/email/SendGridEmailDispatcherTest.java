package com.eventflow.provider.infrastructure.dispatcher.email;

import com.eventflow.provider.domain.model.DispatchResult;
import com.eventflow.provider.domain.model.Provider;
import com.eventflow.provider.domain.model.ProviderCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendGridEmailDispatcherTest {

    @Mock
    private SendGridClient sendGridClient;

    private SendGridEmailDispatcher dispatcher;
    private Provider provider;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        dispatcher = new SendGridEmailDispatcher(sendGridClient);
        notificationId = UUID.randomUUID();
        
        provider = Provider.builder()
                .id(UUID.randomUUID())
                .type(Provider.ProviderType.SENDGRID)
                .channel(Provider.Channel.EMAIL)
                .credentials(ProviderCredentials.builder()
                        .apiKey("SG.test-api-key")
                        .build())
                .status(Provider.Status.ACTIVE)
                .build();
    }

    @Test
    void dispatch_shouldSendEmailSuccessfully() {
        Map<String, Object> content = Map.of(
                "subject", "Test Subject",
                "body", "Test Body"
        );

        when(sendGridClient.sendEmail(any(), any(), any(), any(), any()))
                .thenReturn("msg_12345");

        DispatchResult result = dispatcher.dispatch(
                notificationId,
                provider,
                "user@example.com",
                content
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExternalMessageId()).isEqualTo("msg_12345");
        verify(sendGridClient).sendEmail(any(), eq("user@example.com"), eq("Test Subject"), eq("Test Body"), any());
    }

    @Test
    void dispatch_shouldHandleFailure() {
        Map<String, Object> content = Map.of("subject", "Test", "body", "Body");

        when(sendGridClient.sendEmail(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("SendGrid API error"));

        DispatchResult result = dispatcher.dispatch(
                notificationId,
                provider,
                "user@example.com",
                content
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getException()).isNotNull();
    }

    @Test
    void dispatch_shouldValidateEmailAddress() {
        Map<String, Object> content = Map.of("subject", "Test", "body", "Body");

        DispatchResult result = dispatcher.dispatch(
                notificationId,
                provider,
                "invalid-email",
                content
        );

        assertThat(result.isSuccess()).isFalse();
        verify(sendGridClient, never()).sendEmail(any(), any(), any(), any(), any());
    }
}
