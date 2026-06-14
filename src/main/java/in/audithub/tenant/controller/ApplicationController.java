package in.audithub.tenant.controller;

import in.audithub.common.model.ApiResponse;
import in.audithub.common.security.TenantContext;
import in.audithub.tenant.model.Application;
import in.audithub.tenant.service.ApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final TenantContext tenantContext;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationDto> create(@Valid @RequestBody CreateApplicationRequest request) {
        UUID orgId = tenantContext.getOrganizationId();
        // Fallback or dummy user ID for bootstrapping/API key ingestion context
        UUID userId = tenantContext.getUserId() != null ? tenantContext.getUserId() : UUID.randomUUID();

        Application app = applicationService.create(
                request.getName(),
                request.getEnvironment(),
                request.getDescription(),
                request.getWebhookUrl(),
                orgId,
                userId
        );

        return ApiResponse.success(mapToDto(app));
    }

    @GetMapping
    public ApiResponse<List<ApplicationDto>> list() {
        UUID orgId = tenantContext.getOrganizationId();
        List<Application> apps = applicationService.getByOrganization(orgId);
        List<ApplicationDto> dtos = apps.stream().map(this::mapToDto).toList();
        return ApiResponse.success(dtos);
    }

    private ApplicationDto mapToDto(Application app) {
        return ApplicationDto.builder()
                .id(app.getId())
                .name(app.getName())
                .slug(app.getSlug())
                .description(app.getDescription())
                .environment(app.getEnvironment())
                .status(app.getStatus())
                .createdAt(app.getCreatedAt().toString())
                .build();
    }

    @Data
    public static class CreateApplicationRequest {
        @NotBlank(message = "Application name is required")
        private String name;

        @NotBlank(message = "Environment is required")
        private String environment; // PRODUCTION, STAGING, DEVELOPMENT

        private String description;
        private String webhookUrl;
    }

    @Data
    @lombok.Builder
    public static class ApplicationDto {
        private UUID id;
        private String name;
        private String slug;
        private String description;
        private String environment;
        private String status;
        private String createdAt;
    }
}
