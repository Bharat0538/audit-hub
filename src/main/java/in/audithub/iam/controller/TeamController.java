package in.audithub.iam.controller;

import in.audithub.common.model.ApiResponse;
import in.audithub.common.security.TenantContext;
import in.audithub.iam.model.User;
import in.audithub.iam.model.UserRole;
import in.audithub.iam.model.RoleName;
import in.audithub.iam.repository.UserRepository;
import in.audithub.iam.repository.UserRoleRepository;
import in.audithub.tenant.model.Organization;
import in.audithub.tenant.repository.OrganizationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/team")
@RequiredArgsConstructor
public class TeamController {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantContext tenantContext;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/users")
    public ApiResponse<List<UserWithRoleDto>> listUsers() {
        UUID orgId = tenantContext.getOrganizationId();
        List<UserRole> roles = userRoleRepository.findByOrganizationId(orgId);
        List<UserWithRoleDto> dtos = roles.stream().map(r -> {
            User u = r.getUser();
            return UserWithRoleDto.builder()
                    .id(u.getId())
                    .organizationId(orgId)
                    .email(u.getEmail())
                    .name(u.getName())
                    .avatarUrl(u.getAvatarUrl())
                    .status(u.getStatus())
                    .authProvider(u.getAuthProvider())
                    .mfaEnabled(u.getMfaEnabled())
                    .lastLoginAt(u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : null)
                    .createdAt(u.getCreatedAt().toString())
                    .role(r.getRole().name())
                    .build();
        }).toList();
        return ApiResponse.success(dtos);
    }

    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> invite(@Valid @RequestBody InviteRequest request) {
        UUID orgId = tenantContext.getOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        // Check if user already exists
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            user = User.builder()
                    .organization(org)
                    .email(request.getEmail())
                    .name(request.getEmail().split("@")[0])
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .status("ACTIVE")
                    .authProvider("LOCAL")
                    .mfaEnabled(false)
                    .build();
            user = userRepository.save(user);
        }

        // Add Role
        RoleName roleName;
        try {
            roleName = RoleName.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            roleName = RoleName.VIEWER;
        }

        UserRole role = UserRole.builder()
                .user(user)
                .organization(org)
                .role(roleName)
                .build();
        userRoleRepository.save(role);

        return ApiResponse.success(null);
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeUser(@PathVariable UUID userId) {
        UUID orgId = tenantContext.getOrganizationId();
        List<UserRole> roles = userRoleRepository.findByUserId(userId);
        for (UserRole role : roles) {
            if (role.getOrganization().getId().equals(orgId)) {
                userRoleRepository.delete(role);
            }
        }
    }

    @Data
    public static class InviteRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Role is required")
        private String role;

        private String applicationId;
    }

    @Data
    @lombok.Builder
    public static class UserWithRoleDto {
        private UUID id;
        private UUID organizationId;
        private String email;
        private String name;
        private String avatarUrl;
        private String status;
        private String authProvider;
        private boolean mfaEnabled;
        private String lastLoginAt;
        private String createdAt;
        private String role;
    }
}
