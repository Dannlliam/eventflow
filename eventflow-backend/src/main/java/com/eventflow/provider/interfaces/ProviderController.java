package com.eventflow.provider.interfaces;

import com.eventflow.common.domain.Channel;
import com.eventflow.common.infrastructure.ResourceNotFoundException;
import com.eventflow.common.infrastructure.WorkspaceContextProvider;
import com.eventflow.provider.application.ProviderRepository;
import com.eventflow.provider.application.SaveProviderUseCase;
import com.eventflow.provider.domain.Provider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    private final ProviderRepository providerRepository;
    private final WorkspaceContextProvider workspaceContextProvider;

    public ProviderController(SaveProviderUseCase saveProviderUseCase,
                              ProviderRepository providerRepository,
                              WorkspaceContextProvider workspaceContextProvider) {
        this.saveProviderUseCase = saveProviderUseCase;
        this.providerRepository = providerRepository;
        this.workspaceContextProvider = workspaceContextProvider;
    }

    @PostMapping
    public ResponseEntity<ProviderResponse> createProvider(
            @Valid @RequestBody ProviderRequest request,
            HttpServletRequest servletRequest) {

        UUID workspaceId = workspaceContextProvider.getOptionalWorkspaceId()
            .orElseThrow(() -> new IllegalStateException(
                "Missing workspace context. Authentication is required to create providers."));
        UUID userId = workspaceContextProvider.getOptionalUserId()
            .orElseThrow(() -> new IllegalStateException(
                "Missing user context. Authentication is required to create providers."));

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

        ProviderResponse response = toProviderResponse(provider);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProviderResponse>> listProviders(
            @RequestParam(required = false) String channel,
            HttpServletRequest servletRequest) {
        UUID workspaceId = workspaceContextProvider.getCurrentWorkspaceId();

        log.info("Listing providers for workspaceId={}, channel={}", workspaceId, channel);

        List<Provider> providers;
        if (channel != null && !channel.isBlank()) {
            Channel channelEnum = Channel.fromString(channel);
            providers = providerRepository.findByWorkspaceIdAndChannel(workspaceId, channelEnum);
        } else {
            providers = providerRepository.findByWorkspaceId(workspaceId);
        }

        List<ProviderResponse> responses = providers.stream()
            .map(this::toProviderResponse)
            .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProviderResponse> getProvider(@PathVariable String id,
                                                          HttpServletRequest servletRequest) {
        UUID workspaceId = workspaceContextProvider.getCurrentWorkspaceId();
        UUID providerId = UUID.fromString(id);

        log.info("Getting provider: id={}, workspaceId={}", providerId, workspaceId);

        return providerRepository.findByIdAndWorkspaceId(providerId, workspaceId)
            .map(provider -> ResponseEntity.ok(toProviderResponse(provider)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProviderResponse> updateProvider(@PathVariable String id,
                                                             @Valid @RequestBody ProviderRequest request,
                                                             HttpServletRequest servletRequest) {
        UUID workspaceId = workspaceContextProvider.getCurrentWorkspaceId();
        UUID userId = workspaceContextProvider.getCurrentUserId();
        UUID providerId = UUID.fromString(id);

        log.info("Updating provider: id={}, workspaceId={}", providerId, workspaceId);

        // Verify the provider exists and belongs to the workspace
        Provider existing = providerRepository.findByIdAndWorkspaceId(providerId, workspaceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Provider", providerId.toString()));

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

        return ResponseEntity.ok(toProviderResponse(provider));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProvider(@PathVariable String id,
                                               HttpServletRequest servletRequest) {
        UUID workspaceId = workspaceContextProvider.getCurrentWorkspaceId();
        UUID providerId = UUID.fromString(id);

        log.info("Deleting provider: id={}, workspaceId={}", providerId, workspaceId);

        Provider existing = providerRepository.findByIdAndWorkspaceId(providerId, workspaceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Provider", providerId.toString()));

        providerRepository.deleteById(providerId);

        return ResponseEntity.noContent().build();
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

    private ProviderResponse toProviderResponse(Provider provider) {
        return new ProviderResponse(
            provider.getId().toString(),
            provider.getName(),
            provider.getProviderType().name(),
            provider.getChannel().name(),
            provider.isPrimary(),
            provider.isEnabled(),
            provider.getRateLimit(),
            provider.getRateLimitDurationSeconds()
        );
    }
}