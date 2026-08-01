package com.eventflow.common.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SsrfProtectionServiceTest {

    private SsrfProtectionService ssrfProtectionService;

    @BeforeEach
    void setUp() {
        ssrfProtectionService = new SsrfProtectionService();
    }

    @Test
    void validateUrl_shouldAllowValidPublicUrl() {
        String url = "https://api.example.com/webhook";

        boolean isValid = ssrfProtectionService.validateUrl(url);

        assertThat(isValid).isTrue();
    }

    @Test
    void validateUrl_shouldBlockLocalhostUrl() {
        assertThat(ssrfProtectionService.validateUrl("http://localhost:8080/admin")).isFalse();
        assertThat(ssrfProtectionService.validateUrl("http://127.0.0.1/admin")).isFalse();
        assertThat(ssrfProtectionService.validateUrl("http://127.0.0.1:3000/api")).isFalse();
    }

    @Test
    void validateUrl_shouldBlockPrivateIpRanges() {
        assertThat(ssrfProtectionService.validateUrl("http://192.168.1.1/admin")).isFalse();
        assertThat(ssrfProtectionService.validateUrl("http://10.0.0.1/admin")).isFalse();
        assertThat(ssrfProtectionService.validateUrl("http://172.16.0.1/admin")).isFalse();
    }

    @Test
    void validateUrl_shouldBlockLinkLocalAddresses() {
        assertThat(ssrfProtectionService.validateUrl("http://169.254.1.1/metadata")).isFalse();
    }

    @Test
    void validateUrl_shouldBlockMetadataEndpoints() {
        assertThat(ssrfProtectionService.validateUrl("http://169.254.169.254/latest/meta-data")).isFalse();
    }

    @Test
    void validateUrl_shouldBlockInternalDomains() {
        assertThat(ssrfProtectionService.validateUrl("http://internal.company.local/api")).isFalse();
        assertThat(ssrfProtectionService.validateUrl("http://service.internal/endpoint")).isFalse();
    }

    @Test
    void validateUrl_shouldAllowHttpsUrls() {
        assertThat(ssrfProtectionService.validateUrl("https://api.stripe.com/webhook")).isTrue();
        assertThat(ssrfProtectionService.validateUrl("https://hooks.slack.com/services/xxx")).isTrue();
    }

    @Test
    void validateUrl_shouldBlockFileProtocol() {
        assertThat(ssrfProtectionService.validateUrl("file:///etc/passwd")).isFalse();
    }

    @Test
    void validateUrl_shouldBlockFtpProtocol() {
        assertThat(ssrfProtectionService.validateUrl("ftp://internal-server/file")).isFalse();
    }

    @Test
    void validateUrl_shouldHandleUrlEncodedIps() {
        assertThat(ssrfProtectionService.validateUrl("http://2130706433/admin")).isFalse();
    }

    @Test
    void validateUrl_shouldBlockIpv6Localhost() {
        assertThat(ssrfProtectionService.validateUrl("http://[::1]/admin")).isFalse();
        assertThat(ssrfProtectionService.validateUrl("http://[0:0:0:0:0:0:0:1]/admin")).isFalse();
    }

    @Test
    void validateUrl_shouldThrowExceptionForMalformedUrl() {
        assertThatThrownBy(() -> ssrfProtectionService.validateUrl("not-a-valid-url"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateUrl_shouldThrowExceptionForNullUrl() {
        assertThatThrownBy(() -> ssrfProtectionService.validateUrl(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateUrl_shouldThrowExceptionForEmptyUrl() {
        assertThatThrownBy(() -> ssrfProtectionService.validateUrl(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateUrl_shouldAllowCommonWebhookProviders() {
        assertThat(ssrfProtectionService.validateUrl("https://hooks.zapier.com/hooks/catch/xxx")).isTrue();
        assertThat(ssrfProtectionService.validateUrl("https://discord.com/api/webhooks/xxx")).isTrue();
        assertThat(ssrfProtectionService.validateUrl("https://api.telegram.org/botXXX/sendMessage")).isTrue();
    }

    @Test
    void validateUrl_shouldBlockRedirectAttempts() {
        assertThat(ssrfProtectionService.validateUrl("https://attacker.com/redirect?url=http://localhost")).isFalse();
    }

    @Test
    void validateUrl_shouldHandlePortNumbers() {
        assertThat(ssrfProtectionService.validateUrl("https://api.example.com:8443/webhook")).isTrue();
        assertThat(ssrfProtectionService.validateUrl("http://localhost:8080/admin")).isFalse();
    }
}
