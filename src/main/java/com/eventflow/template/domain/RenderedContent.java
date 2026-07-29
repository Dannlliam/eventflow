package com.eventflow.template.domain;

/**
 * Record representing the result of template rendering.
 */
public record RenderedContent(String subject, String htmlBody, String textBody) {
}