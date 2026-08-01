package com.eventflow.common.infrastructure;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * OWASP HTML sanitizer for email templates.
 * Strips dangerous tags and attributes from rendered email HTML
 * to protect end-users from stored XSS attacks.
 *
 * As specified in the PRD Section 49 - Template Engine / Sanitization (Email Only).
 *
 * Sanitization rules:
 * - Strips: <script>, <iframe>, <object>, <embed>, event handlers (onerror, onload, etc.)
 * - Allows: Basic formatting (b, i, u, em, strong, a, img, p, div, span, table, etc.)
 * - Attributes: href, src, alt, style, class, id
 * - Protocols: http, https, mailto (blocks javascript: and data:)
 */
@Service
public class HtmlSanitizerService {

    private static final Logger log = LoggerFactory.getLogger(HtmlSanitizerService.class);

    private static final Pattern STYLE_PATTERN = Pattern.compile(
        "(javascript|expression|vbscript|data|file)\\s*:",
        Pattern.CASE_INSENSITIVE
    );

    private final PolicyFactory sanitizerPolicy;

    public HtmlSanitizerService() {
        this.sanitizerPolicy = createSanitizerPolicy();
    }

    /**
     * Sanitizes HTML content for safe email rendering.
     * Removes dangerous tags and attributes while preserving formatting.
     *
     * @param html the raw HTML content from rendered template
     * @return sanitized HTML safe for email delivery
     */
    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }

        String sanitized = sanitizerPolicy.sanitize(html);

        // Additional safety: strip any inline event handlers that might have slipped through
        sanitized = sanitized.replaceAll("(?i)\\bon\\w+\\s*=", "data-removed-");

        // Strip javascript: and data: URIs
        sanitized = STYLE_PATTERN.matcher(sanitized).replaceAll("#blocked-");

        log.debug("HTML sanitization: originalLength={}, sanitizedLength={}",
            html.length(), sanitized.length());

        return sanitized;
    }

    /**
     * Creates the OWASP HTML sanitizer policy.
     * Allows common email formatting tags while blocking dangerous elements.
     */
    private PolicyFactory createSanitizerPolicy() {
        PolicyFactory basicFormatting = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.IMAGES)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.STYLES)
            .and(Sanitizers.TABLES);

        // Additional elements commonly used in email templates
        PolicyFactory extendedFormatting = new HtmlPolicyBuilder()
            .allowElements(
                "html", "head", "body", "meta", "title",
                "div", "span", "p", "br", "hr",
                "h1", "h2", "h3", "h4", "h5", "h6",
                "ul", "ol", "li",
                "table", "thead", "tbody", "tfoot", "tr", "th", "td",
                "a", "img",
                "b", "i", "u", "em", "strong", "small", "sub", "sup",
                "pre", "code", "blockquote",
                "center", "font", "marquee"
            )
            .allowAttributes("href", "src", "alt", "title", "width", "height",
                "align", "valign", "border", "cellpadding", "cellspacing",
                "bgcolor", "color", "face", "size")
                .onElements("a", "img", "td", "th", "tr", "table", "font", "center")
            .allowAttributes("target", "rel")
                .onElements("a")
            .allowAttributes("style")
                .onElements("div", "span", "p", "td", "th", "table", "a")
            .allowUrlProtocols("http", "https", "mailto")
            .allowStyling()
            .toFactory();

        return basicFormatting.and(extendedFormatting);
    }

    /**
     * Checks if HTML content contains any dangerous patterns.
     * Used for validation before sanitization.
     *
     * @param html the HTML content to check
     * @return true if the content appears safe
     */
    public boolean isSafe(String html) {
        if (html == null || html.isBlank()) {
            return true;
        }

        String lower = html.toLowerCase();

        // Check for dangerous tags
        if (lower.contains("<script") || lower.contains("<iframe") ||
            lower.contains("<object") || lower.contains("<embed")) {
            return false;
        }

        // Check for event handlers
        Pattern eventHandlerPattern = Pattern.compile("\\bon\\w+\\s*=", Pattern.CASE_INSENSITIVE);
        if (eventHandlerPattern.matcher(lower).find()) {
            return false;
        }

        // Check for javascript: URIs
        if (lower.contains("javascript:") || lower.contains("vbscript:") ||
            lower.contains("data:text/html") || lower.contains("data:application/x")) {
            return false;
        }

        return true;
    }
}