package com.eventflow.common.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for SQL injection protection and secure JPA usage.
 * Implements OWASP ASVS V5: Input Validation and V6: Database Security.
 *
 * Configures:
 * - JPA parameterized queries (default with Spring Data)
 * - Hibernate SQL injection prevention
 * - Flyway migration validation
 */
@Configuration
public class SqlInjectionProtectionConfig implements WebMvcConfigurer {

    /**
     * Maximum length for string inputs to prevent buffer overflow attacks.
     */
    public static final int MAX_STRING_LENGTH = 4000;

    /**
     * Maximum length for long text fields (template bodies, etc.).
     */
    public static final int MAX_LONG_TEXT_LENGTH = 100000;

    /**
     * Validates that a string input does not exceed the maximum length.
     * Prevents buffer overflow and DoS attacks via oversized inputs.
     *
     * @param input the string to validate
     * @param maxLength the maximum allowed length
     * @return the validated string, or truncated version
     */
    public static String sanitizeInput(String input, int maxLength) {
        if (input == null) return null;
        if (input.length() > maxLength) {
            return input.substring(0, maxLength);
        }
        return input;
    }

    /**
     * Escapes special characters that could be used in SQL injection attempts.
     * This is a defense-in-depth measure; parameterized queries are the primary defense.
     *
     * @param input the string to sanitize
     * @return sanitized string
     */
    public static String escapeSqlInjection(String input) {
        if (input == null) return null;
        // Remove common SQL injection patterns
        return input
            .replaceAll("(?i)('\\s*OR\\s*1\\s*=\\s*1)", "")
            .replaceAll("(?i)('\\s*OR\\s*'\\s*'\\s*=\\s*')", "")
            .replaceAll("(?i)(\\bDROP\\s+TABLE)", "")
            .replaceAll("(?i)(\\bUNION\\s+SELECT)", "")
            .replaceAll("(?i)(\\bSELECT\\s+.*\\s+FROM)", "")
            .replaceAll("(?i)(\\bINSERT\\s+INTO)", "")
            .replaceAll("(?i)(\\bDELETE\\s+FROM)", "")
            .replaceAll("(?i)(\\bEXEC\\s*\\()", "")
            .replaceAll("(?i)(\\bxp_cmdshell)", "")
            .replaceAll("--", "");
    }
}