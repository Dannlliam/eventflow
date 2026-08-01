package com.eventflow.template.infrastructure;

import com.eventflow.template.application.TemplateRendererPort;
import com.eventflow.template.domain.RenderedContent;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.StringTemplateSource;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handlebars implementation of the TemplateRendererPort.
 * Renders templates using Handlebars.java with OWASP HTML sanitization for emails.
 */
@Component
public class HandlebarsTemplateRenderer implements TemplateRendererPort {

    private static final Logger log = LoggerFactory.getLogger(HandlebarsTemplateRenderer.class);

    private final Handlebars handlebars;
    private final PolicyFactory htmlSanitizer;

    public HandlebarsTemplateRenderer() {
        this.handlebars = new Handlebars();
        // Configure strict mode to fail on missing variables
        this.handlebars.setInfiniteLoops(false);
        this.handlebars.setPrettyPrint(false);

        // OWASP HTML Sanitizer - strips dangerous tags while preserving safe HTML
        this.htmlSanitizer = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.TABLES)
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.IMAGES)
            .and(Sanitizers.STYLES);
    }

    @Override
    public RenderedContent render(String templateContent, String subjectTemplate,
                                  Map<String, String> variables) {
        try {
            // Render subject
            String subject = "";
            if (subjectTemplate != null && !subjectTemplate.isBlank()) {
                var subjectSource = new StringTemplateSource("subject", subjectTemplate);
                var subjectCompiled = handlebars.compileInline(subjectTemplate);
                subject = subjectCompiled.apply(variables);
            }

            // Render body
            var bodySource = new StringTemplateSource("body", templateContent);
            var bodyCompiled = handlebars.compileInline(templateContent);
            String renderedBody = bodyCompiled.apply(variables);

            // Sanitize HTML for email content
            String sanitizedHtml = htmlSanitizer.sanitize(renderedBody);

            // Generate plain text version (strip HTML tags)
            String textBody = renderedBody
                .replaceAll("<[^>]*>", "")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&nbsp;", " ")
                .trim();

            log.debug("Rendered template: subject={}, bodyLength={}", subject, sanitizedHtml.length());

            return new RenderedContent(subject, sanitizedHtml, textBody);

        } catch (Exception e) {
            log.error("Failed to render template", e);
            throw new RuntimeException("Template rendering failed: " + e.getMessage(), e);
        }
    }
}