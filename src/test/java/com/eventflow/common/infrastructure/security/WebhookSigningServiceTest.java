package com.eventflow.common.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookSigningServiceTest {

    private WebhookSigningService signingService;
    private String secret;

    @BeforeEach
    void setUp() {
        signingService = new WebhookSigningService();
        secret = "webhook-secret-key-12345";
    }

    @Test
    void generateSignature_shouldCreateValidHmacSha256() {
        String payload = "{\"event\":\"notification.delivered\",\"id\":\"123\"}";

        String signature = signingService.generateSignature(payload, secret);

        assertThat(signature).isNotNull();
        assertThat(signature).hasSize(64);
        assertThat(signature).matches("^[a-f0-9]{64}$");
    }

    @Test
    void generateSignature_shouldBeConsistent() {
        String payload = "{\"event\":\"test\"}";

        String sig1 = signingService.generateSignature(payload, secret);
        String sig2 = signingService.generateSignature(payload, secret);

        assertThat(sig1).isEqualTo(sig2);
    }

    @Test
    void generateSignature_shouldBeDifferentForDifferentPayloads() {
        String payload1 = "{\"id\":\"1\"}";
        String payload2 = "{\"id\":\"2\"}";

        String sig1 = signingService.generateSignature(payload1, secret);
        String sig2 = signingService.generateSignature(payload2, secret);

        assertThat(sig1).isNotEqualTo(sig2);
    }

    @Test
    void generateSignature_shouldBeDifferentForDifferentSecrets() {
        String payload = "{\"event\":\"test\"}";
        String secret1 = "secret1";
        String secret2 = "secret2";

        String sig1 = signingService.generateSignature(payload, secret1);
        String sig2 = signingService.generateSignature(payload, secret2);

        assertThat(sig1).isNotEqualTo(sig2);
    }

    @Test
    void verifySignature_shouldReturnTrueForValidSignature() {
        String payload = "{\"event\":\"notification.delivered\"}";
        String signature = signingService.generateSignature(payload, secret);

        boolean isValid = signingService.verifySignature(payload, signature, secret);

        assertThat(isValid).isTrue();
    }

    @Test
    void verifySignature_shouldReturnFalseForInvalidSignature() {
        String payload = "{\"event\":\"notification.delivered\"}";
        String invalidSignature = "invalid-signature-12345";

        boolean isValid = signingService.verifySignature(payload, invalidSignature, secret);

        assertThat(isValid).isFalse();
    }

    @Test
    void verifySignature_shouldReturnFalseForTamperedPayload() {
        String originalPayload = "{\"event\":\"notification.delivered\",\"amount\":100}";
        String signature = signingService.generateSignature(originalPayload, secret);
        String tamperedPayload = "{\"event\":\"notification.delivered\",\"amount\":1000}";

        boolean isValid = signingService.verifySignature(tamperedPayload, signature, secret);

        assertThat(isValid).isFalse();
    }

    @Test
    void verifySignature_shouldReturnFalseForWrongSecret() {
        String payload = "{\"event\":\"test\"}";
        String signature = signingService.generateSignature(payload, secret);
        String wrongSecret = "wrong-secret";

        boolean isValid = signingService.verifySignature(payload, signature, wrongSecret);

        assertThat(isValid).isFalse();
    }

    @Test
    void generateSignature_shouldThrowExceptionForNullPayload() {
        assertThatThrownBy(() -> signingService.generateSignature(null, secret))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateSignature_shouldThrowExceptionForNullSecret() {
        assertThatThrownBy(() -> signingService.generateSignature("{\"test\":1}", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateSignature_shouldHandleEmptyPayload() {
        String signature = signingService.generateSignature("", secret);

        assertThat(signature).isNotNull();
        assertThat(signature).hasSize(64);
    }

    @Test
    void generateSignature_shouldHandleLargePayload() {
        String largePayload = "{\"data\":\"" + "x".repeat(10000) + "\"}";

        String signature = signingService.generateSignature(largePayload, secret);

        assertThat(signature).isNotNull();
        assertThat(signature).hasSize(64);
    }

    @Test
    void generateSignature_shouldHandleSpecialCharacters() {
        String payload = "{\"message\":\"Hello! @#$%^&*()_+{}[]|\\:;<>?,./~`\"}";

        String signature = signingService.generateSignature(payload, secret);

        assertThat(signature).isNotNull();
        boolean isValid = signingService.verifySignature(payload, signature, secret);
        assertThat(isValid).isTrue();
    }

    @Test
    void generateSignature_shouldHandleUnicodeCharacters() {
        String payload = "{\"message\":\"こんにちは 世界\"}";

        String signature = signingService.generateSignature(payload, secret);

        assertThat(signature).isNotNull();
        boolean isValid = signingService.verifySignature(payload, signature, secret);
        assertThat(isValid).isTrue();
    }

    @Test
    void verifySignature_shouldBeTimingAttackSafe() {
        String payload = "{\"event\":\"test\"}";
        String validSignature = signingService.generateSignature(payload, secret);
        String invalidSignature = "0" + validSignature.substring(1);

        long start1 = System.nanoTime();
        signingService.verifySignature(payload, validSignature, secret);
        long time1 = System.nanoTime() - start1;

        long start2 = System.nanoTime();
        signingService.verifySignature(payload, invalidSignature, secret);
        long time2 = System.nanoTime() - start2;

        double ratio = (double) Math.max(time1, time2) / Math.min(time1, time2);
        assertThat(ratio).isLessThan(10.0);
    }
}
