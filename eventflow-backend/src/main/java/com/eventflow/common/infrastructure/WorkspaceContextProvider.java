package com.eventflow.common.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility for extracting workspace and user context from the current request.
 * Works with both API key authentication (sets workspaceId attribute)
 * and JWT OAuth2 authentication (extracts from security context).
 * <p>
 * This replaces all hardcoded UUIDs throughout the codebase.
 */
@Component
public class WorkspaceContextProvider {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceContextProvider.class);

    /**
     * Gets the current workspace ID from the request attributes or security context.
     *
     * @return the workspace UUID, or throws if not available
     * @throws IllegalStateException if no workspace context is found
     */
    public UUID getCurrentWorkspaceId() {
        return extractFromRequest("workspaceId")
            .orElseThrow(() -> new IllegalStateException(
                "No workspace context found in current request. " +
                "Ensure API key authentication or JWT token provides workspaceId."));
    }

    /**
     * Gets the current user ID from the request attributes or security context.
     *
     * @return the user UUID, or throws if not available
     * @throws IllegalStateException if no user context is found
     */
    public UUID getCurrentUserId() {
        return extractFromRequest("userId")
            .orElseThrow(() -> new IllegalStateException(
                "No user context found in current request. " +
                "Ensure authentication provides userId."));
    }

    /**
     * Safely gets the current workspace ID, returning empty if not available.
     */
    public Optional<UUID> getOptionalWorkspaceId() {
        return extractFromRequest("workspaceId");
    }

    /**
     * Safely gets the current user ID, returning empty if not available.
     */
    public Optional<UUID> getOptionalUserId() {
        return extractFromRequest("userId");
    }

    private Optional<UUID> extractFromRequest(String attributeName) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            log.warn("No request attributes available - called outside of request context");
            return Optional.empty();
        }

        HttpServletRequest request = attrs.getRequest();
        String value = (String) request.getAttribute(attributeName);
        if (value == null || value.isBlank()) {
            log.warn("No {} attribute found in request - authentication may not be configured", attributeName);
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            log.error("Invalid {} format in request attribute: {}", attributeName, value);
            return Optional.empty();
        }
    }

    // Also expose the request attributes for GraphQL context
    public static final String WORKSPACE_ID_ATTR = "workspaceId";
    public static final String USER_ID_ATTR = "userId";
}