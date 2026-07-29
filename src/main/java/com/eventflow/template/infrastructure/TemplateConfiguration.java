package com.eventflow.template.infrastructure;

import com.eventflow.template.application.TemplatePublishUseCase;
import com.eventflow.template.application.TemplateRepository;
import com.eventflow.template.application.TemplateRendererPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for the Template bounded context.
 */
@Configuration
public class TemplateConfiguration {

    @Bean
    @Primary
    public TemplateRepository templateRepository(SpringDataTemplateRepository springDataRepository,
                                                   SpringDataTemplateVersionRepository springDataVersionRepository) {
        return new JpaTemplateRepository(springDataRepository, springDataVersionRepository);
    }

    @Bean
    @Primary
    public TemplateRendererPort templateRenderer() {
        return new HandlebarsTemplateRenderer();
    }

    @Bean
    public TemplatePublishUseCase templatePublishUseCase(TemplateRepository templateRepository) {
        return new TemplatePublishUseCase(templateRepository);
    }
}
