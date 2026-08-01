package com.eventflow.common.infrastructure;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter that adds OWASP-recommended security headers to all HTTP responses.
 * Implements the security headers required by OWASP ASVS (Application Security Verification Standard).
 *
 * As specified in the PRD Section 62.3 - OWASP Compliance.
 */
@Component
@Order(1)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // OWASP ASVS V14: HTTP Security Headers
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "0"); // Deprecated but still used by legacy browsers
        httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        httpResponse.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data:; " +
            "font-src 'self'; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none'; " +
            "form-action 'self'; " +
            "base-uri 'self'; " +
            "object-src 'none'"
        );
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        httpResponse.setHeader("Permissions-Policy",
            "camera=(), microphone=(), geolocation=(), interest-cohort=()"
        );
        httpResponse.setHeader("Cache-Control", "no-store, max-age=0");
        httpResponse.setHeader("Pragma", "no-cache");

        // Remove Server header to avoid information disclosure
        httpResponse.setHeader("Server", "");

        chain.doFilter(request, response);
    }
}