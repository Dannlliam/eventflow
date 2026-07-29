package com.eventflow.common.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * HMAC-SHA256 webhook signing service for EventFlow.
 * Signs outgoing webhook payloads to ensure authenticity and integrity.
 * The receiving server validates the signature via the X-EventFlow-Signature header.
 *
 * As specified in the PRD Section 48 - Webhook Design / Security (HMAC Signing).
 *
 * Headers:
 * - X-EventFlow-Signature: HMAC-SHA256 signature of the raw JSON body
 * - X-EventFlow-Timestamp: Unix timestamp when the signature was generated
 * - X-EventFlow-Event-Id: Unique event identifier for idempotency
 */
@Service
public class WebhookSigningService {

    private static final Logger log = LoggerFactory.getLogger(WebhookSigningService.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-EventFlow-Signature";
    private static final String TIMESTAMP_HEADER = "X-EventFlow-Timestamp";
    private static final String EVENT_ID_HEADER = "X-EventFlow-Event-Id";
    private static final long MAX_CLOCK_SKEW_SECONDS = 300; // 5 minutes

    /**
     * Generates an HMAC-SHA256 signature for the given payload and secret.
     * The signature is computed over the combination of timestamp + "." + payload.
     * This prevents replay attacks by binding the signature to a specific time window.
     *
     * @param payload the raw JSON payload bytes
     * @param secret the workspace-specific shared secret
     * @return SigningResult containing the signature, timestamp, and headers
     */
    public SigningResult sign(byte[] payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);

            long timestamp = Instant.now().getEpochSecond();
            String timestampStr = String.valueOf(timestamp);

            // Sign the combination of timestamp + "." + payload
            byte[] dataToSign = (timestampStr + "." + new String(payload, StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
            byte[] signatureBytes = mac.doFinal(dataToSign);

            String signature = HexFormat.of().formatHex(signatureBytes);

            log.debug("Generated webhook signature: timestamp={}", timestamp);

            return new SigningResult(
                signature,
                timestamp,
                SIGNATURE_HEADER,
                TIMESTAMP_HEADER,
                EVENT_ID_HEADER
            );
        } catch (NoSuchAlgorithmException e) {
            log.error("HMAC algorithm not available: {}", e.getMessage());
            throw new IllegalStateException("HMAC-SHA256 algorithm is not available", e);
        } catch (InvalidKeyException e) {
            log.error("Invalid HMAC key: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid HMAC signing key", e);
        }
    }

    /**
     * Verifies an HMAC-SHA256 signature against the given payload and secret.
     * Also checks that the timestamp is within the allowed clock skew window.
     *
     * @param payload the raw JSON payload bytes
     * @param signature the received HMAC signature (hex string)
     * @param timestamp the unix timestamp when the signature was generated
     * @param secret the workspace-specific shared secret
     * @return true if the signature is valid and within the time window
     */
    public boolean verify(byte[] payload, String signature, long timestamp, String secret) {
        // Check timestamp freshness (prevent replay attacks)
        long now = Instant.now().getEpochSecond();
        long age = now - timestamp;

        if (Math.abs(age) > MAX_CLOCK_SKEW_SECONDS) {
            log.warn("Webhook signature timestamp is too old: age={}s, maxSkew={}s",
                age, MAX_CLOCK_SKEW_SECONDS);
            return false;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);

            String timestampStr = String.valueOf(timestamp);
            byte[] dataToVerify = (timestampStr + "." + new String(payload, StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
            byte[] expectedSignatureBytes = mac.doFinal(dataToVerify);

            String expectedSignature = HexFormat.of().formatHex(expectedSignatureBytes);

            boolean valid = constantTimeEquals(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
            );

            if (!valid) {
                log.warn("Webhook signature verification failed: expected={}, received={}",
                    expectedSignature, signature);
            }

            return valid;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Constant-time comparison to prevent timing attacks on HMAC verification.
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * Result of a webhook signing operation.
     */
    public record SigningResult(
        String signature,
        long timestamp,
        String signatureHeader,
        String timestampHeader,
        String eventIdHeader
    ) {}
}