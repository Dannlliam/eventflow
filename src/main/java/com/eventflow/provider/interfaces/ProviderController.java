package com.eventflow.provider.interfaces;

import com.eventflow.provider.application.SaveProviderUseCase;
import com.eventflow.provider.domain.Provider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing provider configurations.
 * POST /api/v1/providers - Create a new provider configuration.
 * GET /api/v1/providers - List provider configurations.
 */
@RestController
@RequestMapping("/api/v1/providers")
public class ProviderController {

    private static final Logger log = LoggerFactory.getLogger(ProviderController.class);

    private final SaveProviderUseCase saveProviderUseCase;

    public ProviderController(SaveProviderUseCase saveProviderUseCase) {
        this.saveProviderUseCase = saveProviderUseCase;
    }

    @PostMapping
    public ResponseEntity<ProviderResponse> createProvider(
            @Valid @RequestBody ProviderRequest request,
            HttpServletRequest servletRequest) {

        String workspaceIdStr = (String) servletRequest.getAttribute("workspaceId");
        UUID workspaceId = workspaceIdStr != null ? UUID.fromString(workspaceIdStr) : UUID.randomUUID();
        String userIdStr = (String) servletRequest.getAttribute("userId");
        UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : UUID.randomUUID();

        log.info("Creating provider: name={}, type={}, channel={}, workspaceId={}",
            request.name(), request.providerType(), request.channel(), workspaceId);

        SaveProviderUseCase.SaveProviderCommand command = new SaveProviderUseCase.SaveProviderCommand(
            workspaceId,
            userId,
            request.name(),
            request.providerType(),
            request.channel(),
            request.isPrimary(),
            request.rateLimit(),
            request.rateLimitDurationSeconds(),
            request.credentials(),
            request.settings()
        );

        Provider provider = saveProviderUseCase.execute(command);

        ProviderResponse response = new ProviderResponse(
            provider.getId().toString(),
            provider.getName(),
            provider.getProviderType().name(),
            provider.getChannel().name(),
            provider.isPrimary(),
            provider.isEnabled(),
            provider.getRateLimit(),
            provider.getRateLimitDurationSeconds()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Request/Response DTOs

    public record ProviderRequest(
        @NotBlank String name,
        @NotBlank String providerType,
        @NotBlank String channel,
        boolean isPrimary,
        @Min(1) int rateLimit,
        @Min(1) int rateLimitDurationSeconds,
        Map<String, String> credentials,
        Map<String, String> settings
    ) {}

    public record ProviderResponse(
        String id,
        String name,
        String providerType,
        String channel,
        boolean primary,
        boolean enabled,
        int rateLimit,
        int rateLimitDurationSeconds
    ) {}
}