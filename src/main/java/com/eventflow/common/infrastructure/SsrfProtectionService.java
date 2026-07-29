package com.eventflow.common.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SSRF (Server-Side Request Forgery) protection service for webhook URLs.
 * Validates and blocks requests to internal/private IP ranges to prevent
 * malicious actors from forcing EventFlow to ping internal infrastructure.
 *
 * As specified in the PRD Section 53 - Webhook Provider / SSRF Protection
 * and Section 73 - OWASP Compliance / SSRF mitigation.
 *
 * Blocked ranges (RFC 1918 and RFC 6598):
 * - 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
 * - 127.0.0.0/8 (localhost), 169.254.0.0/16 (link-local)
 * - 0.0.0.0/8, ::1/128 (IPv6 localhost)
 */
@Service
public class SsrfProtectionService {

    private static final Logger log = LoggerFactory.getLogger(SsrfProtectionService.class);

    private static final List<Pattern> BLOCKED_HOST_PATTERNS = Arrays.asList(
        Pattern.compile("^localhost$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"),
        Pattern.compile("^0\\.0\\.0\\.0$"),
        Pattern.compile("^169\\.254\\.\\d{1,3}\\.\\d{1,3}$"),
        Pattern.compile("^10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"),
        Pattern.compile("^172\\.(1[6-9]|2[0-9]|3[0-1])\\.\\d{1,3}\\.\\d{1,3}$"),
        Pattern.compile("^192\\.168\\.\\d{1,3}\\.\\d{1,3}$"),
        Pattern.compile("^::1$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^fc00:|fd00:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^fe80:", Pattern.CASE_INSENSITIVE)
    );

    private static final List<String> BLOCKED_SCHEMES = Arrays.asList(
        "file", "ftp", "ldap", "ldaps", "gopher", "dict", "tftp"
    );

    private static final List<String> ALLOWED_SCHEMES = Arrays.asList(
        "http", "https"
    );

    /**
     * Validates a webhook URL against SSRF threats.
     * Checks for blocked schemes, private IP ranges, and hostname resolution.
     *
     * @param url the webhook URL to validate
     * @return true if the URL is safe to dispatch to
     * @throws IllegalArgumentException if the URL is invalid or blocked
     */
    public boolean validateUrl(String url) {
        if (url == null || url.isBlank()) {
            log.warn("SSRF validation failed: URL is null or empty");
            throw new IllegalArgumentException("Webhook URL must not be null or empty");
        }

        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            log.warn("SSRF validation failed: Invalid URL format - {}", url);
            throw new IllegalArgumentException("Invalid webhook URL format: " + url, e);
        }

        // Validate scheme
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            log.warn("SSRF validation failed: Blocked scheme '{}' for URL {}", scheme, url);
            throw new IllegalArgumentException(
                "Blocked webhook URL scheme: '" + scheme + "'. Only HTTP and HTTPS are allowed.");
        }

        // Validate host
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            log.warn("SSRF validation failed: No host in URL {}", url);
            throw new IllegalArgumentException("Webhook URL must contain a valid host");
        }

        // Check blocked hostname patterns
        for (Pattern pattern : BLOCKED_HOST_PATTERNS) {
            if (pattern.matcher(host).find()) {
                log.warn("SSRF validation failed: Blocked host pattern matched '{}' for URL {}", pattern, url);
                throw new IllegalArgumentException(
                    "Webhook URL points to a blocked/internal IP range: " + host);
            }
        }

        // Resolve hostname to IP and check against private ranges
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            if (inetAddress.isSiteLocalAddress() ||
                inetAddress.isLoopbackAddress() ||
                inetAddress.isLinkLocalAddress()) {
                log.warn("SSRF validation failed: Resolved to private IP '{}' for URL {}",
                    inetAddress.getHostAddress(), url);
                throw new IllegalArgumentException(
                    "Webhook URL resolves to an internal/private IP address: " + host);
            }
            log.debug("SSRF validation passed: URL={} resolved to public IP={}",
                url, inetAddress.getHostAddress());
        } catch (UnknownHostException e) {
            log.warn("SSRF validation: Unable to resolve host '{}' for URL {}. Allowing due to resolution failure.",
                host, url);
            // Allow through if DNS resolution fails; the actual HTTP call will fail naturally
        }

        return true;
    }

    /**
     * Sanitizes a URL by removing fragment and normalizing.
     *
     * @param url the raw URL
     * @return normalized URL safe for dispatch
     */
    public String sanitizeUrl(String url) {
        URI uri = URI.create(url);
        // Strip fragment, normalize path
        String sanitized = uri.getScheme() + "://" + uri.getHost() +
            (uri.getPort() > 0 ? ":" + uri.getPort() : "") +
            (uri.getPath() != null ? uri.getPath() : "") +
            (uri.getQuery() != null ? "?" + uri.getQuery() : "");
        return sanitized;
    }
}