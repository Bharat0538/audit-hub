package in.audithub.tenant.controller;

import in.audithub.common.model.ApiResponse;
import in.audithub.common.security.TenantContext;
import in.audithub.tenant.model.ApiKey;
import in.audithub.tenant.repository.ApiKeyRepository;
import in.audithub.tenant.service.ApiKeyService;
import in.audithub.tenant.service.ApiKeyService.ApiKeyCreatedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final ApiKeyRepository apiKeyRepository;
    private final TenantContext tenantContext;

    @GetMapping
    public ApiResponse<List<ApiKeyDto>> list() {
        UUID orgId = tenantContext.getOrganizationId();
        List<ApiKey> keys = apiKeyRepository.findByOrganizationId(orgId);
        List<ApiKeyDto> dtos = keys.stream()
                .map(k -> ApiKeyDto.builder()
                        .id(k.getId())
                        .name(k.getName())
                        .keyPrefix(k.getKeyPrefix())
                        .keyType(k.getKeyType())
                        .isActive(k.getIsActive())
                        .lastUsedAt(k.getLastUsedAt() != null ? k.getLastUsedAt().toString() : null)
                        .expiresAt(k.getExpiresAt() != null ? k.getExpiresAt().toString() : null)
                        .createdAt(k.getCreatedAt().toString())
                        .build())
                .toList();
        return ApiResponse.success(dtos);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApiKeyCreatedResponse> create(@Valid @RequestBody CreateKeyRequest request) {
        UUID orgId = tenantContext.getOrganizationId();
        UUID userId = tenantContext.getUserId() != null ? tenantContext.getUserId() : UUID.randomUUID();

        UUID appId = request.getApplicationId() != null ? UUID.fromString(request.getApplicationId()) : null;
        Instant expires = request.getExpiresAt() != null ? Instant.parse(request.getExpiresAt()) : null;

        ApiKeyCreatedResponse created = apiKeyService.generate(
                request.getName(),
                orgId,
                appId,
                request.getKeyType(),
                request.getScopes(),
                expires,
                userId
        );
        return ApiResponse.success(created);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id) {
        UUID orgId = tenantContext.getOrganizationId();
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key not found"));

        if (!key.getOrganization().getId().equals(orgId)) {
            throw new IllegalArgumentException("API Key does not belong to organization");
        }

        key.setIsActive(false);
        apiKeyRepository.save(key);
    }

    @Data
    public static class CreateKeyRequest {
        @NotBlank(message = "Key name is required")
        private String name;

        private String applicationId;

        @NotBlank(message = "Key type is required")
        private String keyType; // WRITE, READ, ADMIN

        private List<String> scopes;
        private String expiresAt;
    }

    @Data
    @lombok.Builder
    public static class ApiKeyDto {
        private UUID id;
        private String name;
        private String keyPrefix;
        private String keyType;
        private boolean isActive;
        private String lastUsedAt;
        private String expiresAt;
        private String createdAt;
    }
}
