package com.eventflow.common.infrastructure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for EventFlow REST API documentation.
 * Exposes the API specification at /v3/api-docs and Swagger UI at /swagger-ui.html.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI eventFlowOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("EventFlow API")
                .description("Enterprise-grade event-driven notification orchestration platform.\n\n" +
                    "## Bounded Contexts\n" +
                    "- **Notification Ingestion**: POST /api/v1/notifications\n" +
                    "- **Identity Management**: Users, Roles, API Keys\n" +
                    "- **Template Management**: Versioned templates with Handlebars rendering\n" +
                    "- **Provider Configuration**: Multi-channel dispatchers (SendGrid, Twilio, FCM, Webhook)\n" +
                    "- **Analytics & Audit**: Delivery metrics, materialized views, immutable audit trail\n\n" +
                    "## Authentication\n" +
                    "- API Keys via `Authorization` header (prefix `ef_`)\n" +
                    "- OAuth2 JWT Bearer tokens for admin dashboard")
                .version("1.0.0")
                .contact(new Contact()
                    .name("EventFlow Team")
                    .email("support@eventflow.io")
                    .url("https://eventflow.io"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://eventflow.io/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local development server"),
                new Server()
                    .url("https://api.eventflow.io")
                    .description("Production server")
            ))
            .tags(List.of(
                new Tag().name("Notifications").description("Notification ingestion and status queries"),
                new Tag().name("Templates").description("Template CRUD and version management"),
                new Tag().name("Providers").description("Provider configuration and health checks"),
                new Tag().name("Analytics").description("Delivery analytics and statistics"),
                new Tag().name("Admin").description("Administrative operations (DLQ replay, config)"),
                new Tag().name("Health").description("Health checks and readiness probes")
            ));
    }
}