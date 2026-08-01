package com.eventflow.provider.infrastructure.dispatcher.sms;

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
class TwilioSmsDispatcherTest {

    @Mock
    private TwilioClient twilioClient;

    private TwilioSmsDispatcher dispatcher;
    private Provider provider;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        dispatcher = new TwilioSmsDispatcher(twilioClient);
        notificationId = UUID.randomUUID();
        
        provider = Provider.builder()
                .id(UUID.randomUUID())
                .type(Provider.ProviderType.TWILIO)
                .channel(Provider.Channel.SMS)
                .credentials(ProviderCredentials.builder()
                        .accountSid("AC123")
                        .authToken("token")
                        .build())
                .status(Provider.Status.ACTIVE)
                .build();
    }

    @Test
    void dispatch_shouldSendSmsSuccessfully() {
        Map<String, Object> content = Map.of("body", "Your verification code is 123456");

        when(twilioClient.sendSms(any(), any(), any(), any()))
                .thenReturn("SM9876543210");

        DispatchResult result = dispatcher.dispatch(
                notificationId,
                provider,
                "+1234567890",
                content
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExternalMessageId()).isEqualTo("SM9876543210");
        verify(twilioClient).sendSms(any(), any(), eq("+1234567890"), eq("Your verification code is 123456"));
    }

    @Test
    void dispatch_shouldHandleFailure() {
        Map<String, Object> content = Map.of("body", "Test");

        when(twilioClient.sendSms(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Twilio API error"));

        DispatchResult result = dispatcher.dispatch(
                notificationId,
                provider,
                "+1234567890",
                content
        );

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void dispatch_shouldValidatePhoneNumber() {
        Map<String, Object> content = Map.of("body", "Test");

        DispatchResult result = dispatcher.dispatch(
                notificationId,
                provider,
                "invalid-phone",
                content
        );

        assertThat(result.isSuccess()).isFalse();
        verify(twilioClient, never()).sendSms(any(), any(), any(), any());
    }
}
