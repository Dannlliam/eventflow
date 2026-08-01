package com.eventflow.common.infrastructure;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that adds structured logging context to every request.
 * Injects traceId, workspaceId, and eventId into the MDC (Mapped Diagnostic Context)
 * for correlation across all log statements.
 *
 * As specified in the PRD Section 32 - Logging & Observability.
 */
@Component
@Order(0)
public class StructuredLoggingConfig implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String WORKSPACE_ID_HEADER = "X-Workspace-Id";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // Extract or generate correlation IDs
            String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = "trace_" + UUID.randomUUID().toString().substring(0, 15);
            }

            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = "corr_" + UUID.randomUUID().toString().substring(0, 15);
            }

            String workspaceId = httpRequest.getHeader(WORKSPACE_ID_HEADER);
            if (workspaceId == null) {
                workspaceId = (String) httpRequest.getAttribute("workspaceId");
            }

            // Set MDC context
            MDC.put("traceId", traceId);
            MDC.put("correlationId", correlationId);
            MDC.put("workspaceId", workspaceId != null ? workspaceId : "unknown");
            MDC.put("requestPath", httpRequest.getRequestURI());
            MDC.put("requestMethod", httpRequest.getMethod());

            chain.doFilter(request, response);
        } finally {
            // Clear MDC context after request completes
            MDC.clear();
        }
    }
}