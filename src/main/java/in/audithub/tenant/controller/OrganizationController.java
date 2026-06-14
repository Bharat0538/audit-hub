package in.audithub.tenant.controller;

import in.audithub.common.model.ApiResponse;
import in.audithub.common.security.TenantContext;
import in.audithub.tenant.model.Organization;
import in.audithub.tenant.repository.OrganizationRepository;
import in.audithub.tenant.service.OrganizationService;
import in.audithub.tenant.service.OrganizationService.OnboardingResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationRepository organizationRepository;
    private final TenantContext tenantContext;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OnboardingResponse> signup(@Valid @RequestBody SignupRequest request) {
        OnboardingResponse response = organizationService.create(
                request.getName(),
                request.getContactEmail(),
                request.getPassword(),
                request.getGstNumber()
        );
        return ApiResponse.success(response);
    }

    /**
     * GET /v1/organizations/me — returns the current tenant's organization details.
     */
    @GetMapping("/me")
    public ApiResponse<OrgDto> getCurrent() {
        UUID orgId = tenantContext.getOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        return ApiResponse.success(mapToDto(org));
    }

    /**
     * PATCH /v1/organizations/me — update mutable org fields.
     */
    @PatchMapping("/me")
    public ApiResponse<OrgDto> update(@RequestBody UpdateOrgRequest request) {
        UUID orgId = tenantContext.getOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        if (request.getName() != null) org.setName(request.getName());
        if (request.getDisplayName() != null) org.setDisplayName(request.getDisplayName());
        if (request.getContactEmail() != null) org.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null) org.setContactPhone(request.getContactPhone());
        if (request.getGstNumber() != null) org.setGstNumber(request.getGstNumber());

        org = organizationRepository.save(org);
        return ApiResponse.success(mapToDto(org));
    }

    /**
     * GET /v1/organizations/me/usage — returns simple usage counters for the current org.
     */
    @GetMapping("/me/usage")
    public ApiResponse<Map<String, Object>> getUsage() {
        UUID orgId = tenantContext.getOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        Map<String, Object> usage = Map.of(
                "plan", org.getPlan() != null ? org.getPlan() : "FREE",
                "retentionDays", org.getRetentionDays() != null ? org.getRetentionDays() : 7,
                "maxEventsPerMonth", org.getMaxEventsPerMonth() != null ? org.getMaxEventsPerMonth() : 10000L,
                "maxApplications", org.getMaxApplications() != null ? org.getMaxApplications() : 1
        );
        return ApiResponse.success(usage);
    }

    private OrgDto mapToDto(Organization org) {
        return OrgDto.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .displayName(org.getDisplayName())
                .plan(org.getPlan())
                .status(org.getStatus())
                .country(org.getCountry())
                .contactEmail(org.getContactEmail())
                .contactPhone(org.getContactPhone())
                .gstNumber(org.getGstNumber())
                .retentionDays(org.getRetentionDays())
                .maxEventsPerMonth(org.getMaxEventsPerMonth())
                .maxApplications(org.getMaxApplications())
                .createdAt(org.getCreatedAt() != null ? org.getCreatedAt().toString() : null)
                .build();
    }

    @Data
    @Builder
    public static class OrgDto {
        private UUID id;
        private String name;
        private String slug;
        private String displayName;
        private String plan;
        private String status;
        private String country;
        private String contactEmail;
        private String contactPhone;
        private String gstNumber;
        private Integer retentionDays;
        private Long maxEventsPerMonth;
        private Integer maxApplications;
        private String createdAt;
    }

    @Data
    public static class UpdateOrgRequest {
        private String name;
        private String displayName;
        private String contactEmail;
        private String contactPhone;
        private String gstNumber;
    }

    @Data
    public static class SignupRequest {
        @NotBlank(message = "Company name is required")
        private String name;

        @NotBlank(message = "Contact email is required")
        @Email(message = "Invalid email format")
        private String contactEmail;

        @NotBlank(message = "Password is required")
        private String password;

        private String gstNumber;
    }
}
