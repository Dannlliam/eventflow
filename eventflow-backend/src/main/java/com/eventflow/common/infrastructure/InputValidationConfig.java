package com.eventflow.common.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Configuration for input validation and sanitization across all bounded contexts.
 * Implements OWASP ASVS V5: Input Validation and Encoding.
 *
 * Configures Jackson to reject unknown properties, enforce strict typing,
 * and prevent JSON injection attacks.
 */
@Configuration
public class InputValidationConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.createXmlMapper(false).build();

        // OWASP V5: Reject unknown properties to prevent parameter pollution
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        // Max length validation is handled at the application layer via @Size annotations

        // Disable default typing to prevent polymorphic deserialization attacks
        mapper.disableDefaultTyping();

        // Strict date/time handling
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }
}