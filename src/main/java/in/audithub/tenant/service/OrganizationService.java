package in.audithub.tenant.service;

import in.audithub.iam.model.RoleName;
import in.audithub.iam.model.User;
import in.audithub.iam.model.UserRole;
import in.audithub.iam.repository.UserRepository;
import in.audithub.iam.repository.UserRoleRepository;
import in.audithub.tenant.model.Application;
import in.audithub.tenant.model.Organization;
import in.audithub.tenant.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ApplicationService applicationService;
    private final ApiKeyService apiKeyService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public OnboardingResponse create(String name, String contactEmail, String password, String gstNumber) {
        if (userRepository.findByEmail(contactEmail).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        String slug = slugify(name);
        if (organizationRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        Organization org = Organization.builder()
                .name(name)
                .slug(slug)
                .displayName(name)
                .plan("FREE")
                .status("ACTIVE")
                .contactEmail(contactEmail)
                .gstNumber(gstNumber)
                .maxEventsPerMonth(10000L)
                .maxApplications(5)
                .retentionDays(7)
                .build();

        Organization savedOrg = organizationRepository.save(org);

        User user = User.builder()
                .organization(savedOrg)
                .email(contactEmail)
                .name(name)
                .passwordHash(passwordEncoder.encode(password))
                .status("ACTIVE")
                .authProvider("LOCAL")
                .build();

        User savedUser = userRepository.save(user);

        UserRole role = UserRole.builder()
                .user(savedUser)
                .organization(savedOrg)
                .role(RoleName.OWNER)
                .build();

        userRoleRepository.save(role);

        Application defaultApp = applicationService.create(
                "My First App",
                "PRODUCTION",
                "Default application automatically generated",
                null,
                savedOrg.getId(),
                savedUser.getId()
        );

        ApiKeyService.ApiKeyCreatedResponse apiKey = apiKeyService.generate(
                "Default Write Key",
                savedOrg.getId(),
                defaultApp.getId(),
                "WRITE",
                List.of("audit:write"),
                null,
                savedUser.getId()
        );

        return OnboardingResponse.builder()
                .organizationId(savedOrg.getId())
                .userId(savedUser.getId())
                .slug(savedOrg.getSlug())
                .plainApiKey(apiKey.getPlainTextKey())
                .message("Verification email sent to " + contactEmail)
                .build();
    }

    public Organization getById(UUID id) {
        return organizationRepository.findById(id).orElse(null);
    }

    private String slugify(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .trim();
    }

    @lombok.Value
    @lombok.Builder
    public static class OnboardingResponse {
        UUID organizationId;
        UUID userId;
        String slug;
        String plainApiKey;
        String message;
    }
}
