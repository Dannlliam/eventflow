package com.eventflow.common.infrastructure;

import com.eventflow.identity.application.ApiKeyRepository;
import com.eventflow.identity.domain.ApiKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter for API key authentication on the ingestion endpoint.
 * API keys are passed as Bearer tokens in the Authorization header.
 * The key is hashed with SHA-256 and looked up in the database.
 */
@Component
@Order(1)
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String API_KEY_HEADER = "Authorization";

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Only apply to the ingestion API
        if (!path.startsWith("/api/v1/notifications")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(API_KEY_HEADER);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid API key\"}");
            return;
        }

        String rawKey = authHeader.substring(7).trim();
        String keyHash = ApiKey.hashKey(rawKey);

        var apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);
        if (apiKeyOpt.isEmpty() || !apiKeyOpt.get().isActive()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid or deactivated API key\"}");
            return;
        }

        ApiKey apiKey = apiKeyOpt.get();
        apiKey.recordUsage();
        apiKeyRepository.save(apiKey);

        // Set workspace context for downstream processing
        request.setAttribute("workspaceId", apiKey.getWorkspaceId().toString());

        filterChain.doFilter(request, response);
    }
}