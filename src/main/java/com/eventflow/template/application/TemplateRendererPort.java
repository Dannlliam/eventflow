package com.eventflow.template.application;

import com.eventflow.template.domain.RenderedContent;
import java.util.Map;

/**
 * Port for template rendering.
 * Implementations handle compilation and interpolation of template content.
 */
public interface TemplateRendererPort {
    RenderedContent render(String templateContent, String subjectTemplate, Map<String, String> variables);
}