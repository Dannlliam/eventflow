package com.eventflow.common.infrastructure.security;

import com.eventflow.identity.domain.model.ApiKey;
import com.eventflow.identity.domain.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ApiKeyAuthenticationFilter filter;

    private UUID workspaceId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthenticationFilter(apiKeyRepository);
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void doFilter_shouldAuthenticateRequest_whenValidApiKeyProvided() throws ServletException, IOException {
        // Arrange
        String apiKeyValue = "ef_test_abc123xyz456";
        String apiKeyHash = computeHash(apiKeyValue);

        ApiKey validApiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .workspaceId(workspaceId)
                .name("Test API Key")
                .keyPrefix("ef_test")
                .keyHash(apiKeyHash)
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(request.getHeader("X-API-Key")).thenReturn(apiKeyValue);
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");
        when(apiKeyRepository.findByKeyHash(apiKeyHash)).thenReturn(Optional.of(validApiKey));

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(request).setAttribute("workspaceId", workspaceId);
        verify(request).setAttribute("userId", userId);
        verify(request).setAttribute("authenticated", true);
        verify(apiKeyRepository).updateLastUsedAt(validApiKey.getId(), any(Instant.class));
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilter_shouldReject_whenApiKeyIsNull() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).getWriter();
        verify(filterChain, never()).doFilter(any(), any());
        verify(apiKeyRepository, never()).findByKeyHash(anyString());
    }

    @Test
    void doFilter_shouldReject_whenApiKeyIsInvalid() throws ServletException, IOException {
        // Arrange
        String invalidApiKey = "invalid_key_123";
        String hash = computeHash(invalidApiKey);

        when(request.getHeader("X-API-Key")).thenReturn(invalidApiKey);
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");
        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.empty());

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
        verify(request, never()).setAttribute(eq("workspaceId"), any());
    }

    @Test
    void doFilter_shouldReject_whenApiKeyIsRevoked() throws ServletException, IOException {
        // Arrange
        String apiKeyValue = "ef_test_abc123xyz456";
        String apiKeyHash = computeHash(apiKeyValue);

        ApiKey revokedApiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .workspaceId(workspaceId)
                .name("Revoked Key")
                .keyPrefix("ef_test")
                .keyHash(apiKeyHash)
                .status(ApiKey.Status.REVOKED)
                .createdAt(Instant.now())
                .build();

        when(request.getHeader("X-API-Key")).thenReturn(apiKeyValue);
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");
        when(apiKeyRepository.findByKeyHash(apiKeyHash)).thenReturn(Optional.of(revokedApiKey));

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
        verify(apiKeyRepository, never()).updateLastUsedAt(any(), any());
    }

    @Test
    void doFilter_shouldAcceptBearerToken_inAuthorizationHeader() throws ServletException, IOException {
        // Arrange
        String apiKeyValue = "ef_test_abc123xyz456";
        String apiKeyHash = computeHash(apiKeyValue);

        ApiKey validApiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .workspaceId(workspaceId)
                .name("Test API Key")
                .keyPrefix("ef_test")
                .keyHash(apiKeyHash)
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + apiKeyValue);
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");
        when(apiKeyRepository.findByKeyHash(apiKeyHash)).thenReturn(Optional.of(validApiKey));

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(request).setAttribute("workspaceId", workspaceId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldUpdateLastUsedTimestamp() throws ServletException, IOException {
        // Arrange
        String apiKeyValue = "ef_test_abc123xyz456";
        String apiKeyHash = computeHash(apiKeyValue);

        ApiKey validApiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .workspaceId(workspaceId)
                .name("Test API Key")
                .keyPrefix("ef_test")
                .keyHash(apiKeyHash)
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .lastUsedAt(null)
                .build();

        when(request.getHeader("X-API-Key")).thenReturn(apiKeyValue);
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");
        when(apiKeyRepository.findByKeyHash(apiKeyHash)).thenReturn(Optional.of(validApiKey));

        Instant before = Instant.now();

        // Act
        filter.doFilter(request, response, filterChain);

        Instant after = Instant.now();

        // Assert
        verify(apiKeyRepository).updateLastUsedAt(eq(validApiKey.getId()), any(Instant.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldSkipAuthentication_forPublicEndpoints() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getHeader("X-API-Key")).thenReturn(null);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(apiKeyRepository, never()).findByKeyHash(anyString());
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void doFilter_shouldSkipAuthentication_forSwaggerEndpoints() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/swagger-ui.html");
        when(request.getHeader("X-API-Key")).thenReturn(null);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(apiKeyRepository, never()).findByKeyHash(anyString());
    }

    @Test
    void doFilter_shouldSkipAuthentication_forGraphQLIntrospection() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/graphql");
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getMethod()).thenReturn("POST");
        when(request.getReader()).thenReturn(mock(java.io.BufferedReader.class));

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert - GraphQL typically handles its own auth, so filter may pass through
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldHashApiKey_beforeLookup() throws ServletException, IOException {
        // Arrange
        String apiKeyValue = "ef_prod_xyz987abc654";
        String expectedHash = computeHash(apiKeyValue);

        when(request.getHeader("X-API-Key")).thenReturn(apiKeyValue);
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");
        when(apiKeyRepository.findByKeyHash(expectedHash)).thenReturn(Optional.empty());

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(apiKeyRepository).findByKeyHash(expectedHash);
        verify(apiKeyRepository, never()).findByKeyHash(apiKeyValue); // Plain key never used
    }

    @Test
    void doFilter_shouldReject_whenApiKeyPrefixIsMissing() throws ServletException, IOException {
        // Arrange
        String invalidKey = "abc123"; // Missing "ef_" prefix
        when(request.getHeader("X-API-Key")).thenReturn(invalidKey);
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_shouldHandleMultipleValidApiKeys() throws ServletException, IOException {
        // Arrange
        String apiKey1 = "ef_test_key1";
        String apiKey2 = "ef_test_key2";
        String hash1 = computeHash(apiKey1);
        String hash2 = computeHash(apiKey2);

        ApiKey validKey1 = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .workspaceId(workspaceId)
                .name("Key 1")
                .keyPrefix("ef_test")
                .keyHash(hash1)
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        ApiKey validKey2 = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .workspaceId(workspaceId)
                .name("Key 2")
                .keyPrefix("ef_test")
                .keyHash(hash2)
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(request.getRequestURI()).thenReturn("/api/v1/notifications");
        when(apiKeyRepository.findByKeyHash(hash1)).thenReturn(Optional.of(validKey1));
        when(apiKeyRepository.findByKeyHash(hash2)).thenReturn(Optional.of(validKey2));

        // Act - First request with key1
        when(request.getHeader("X-API-Key")).thenReturn(apiKey1);
        filter.doFilter(request, response, filterChain);

        // Act - Second request with key2
        reset(request, response, filterChain);
        when(request.getHeader("X-API-Key")).thenReturn(apiKey2);
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");
        when(apiKeyRepository.findByKeyHash(hash2)).thenReturn(Optional.of(validKey2));
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(apiKeyRepository).findByKeyHash(hash1);
        verify(apiKeyRepository).findByKeyHash(hash2);
        verify(filterChain, times(2)).doFilter(any(), any());
    }

    @Test
    void doFilter_shouldSetAuthenticationContext_correctly() throws ServletException, IOException {
        // Arrange
        String apiKeyValue = "ef_test_abc123xyz456";
        String apiKeyHash = computeHash(apiKeyValue);

        ApiKey validApiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .workspaceId(workspaceId)
                .name("Test API Key")
                .keyPrefix("ef_test")
                .keyHash(apiKeyHash)
                .status(ApiKey.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(request.getHeader("X-API-Key")).thenReturn(apiKeyValue);
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");
        when(apiKeyRepository.findByKeyHash(apiKeyHash)).thenReturn(Optional.of(validApiKey));

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(request).setAttribute("workspaceId", workspaceId);
        verify(request).setAttribute("userId", userId);
        verify(request).setAttribute("authenticated", true);
        verify(request).setAttribute("apiKeyId", validApiKey.getId());
    }

    // Helper method to simulate SHA-256 hashing
    private String computeHash(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
