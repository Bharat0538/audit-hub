package in.audithub.tenant.service;

import in.audithub.common.exception.UnauthorizedException;
import in.audithub.tenant.model.ApiKey;
import in.audithub.tenant.model.Application;
import in.audithub.tenant.model.Organization;
import in.audithub.tenant.repository.ApiKeyRepository;
import in.audithub.tenant.repository.ApplicationRepository;
import in.audithub.tenant.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final OrganizationRepository organizationRepository;
    private final ApplicationRepository applicationRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    public ApiKeyCreatedResponse generate(String name, UUID organizationId, UUID applicationId, String keyType, List<String> scopes, Instant expiresAt, UUID createdBy) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        Application app = null;
        if (applicationId != null) {
            app = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found"));
            if (!app.getOrganization().getId().equals(organizationId)) {
                throw new IllegalArgumentException("Application does not belong to organization");
            }
        }

        String prefix = app != null && "DEVELOPMENT".equals(app.getEnvironment()) ? "ah_test_" : "ah_live_";
        String randomKey = generateRandomString(32);
        String fullKey = prefix + randomKey;
        String keyPrefix = fullKey.substring(0, 10);
        String keyHash = passwordEncoder.encode(fullKey);

        ApiKey apiKey = ApiKey.builder()
                .organization(org)
                .application(app)
                .name(name)
                .keyPrefix(keyPrefix)
                .keyHash(keyHash)
                .keyType(keyType)
                .scopes(scopes)
                .expiresAt(expiresAt)
                .createdBy(createdBy)
                .isActive(true)
                .build();

        ApiKey saved = apiKeyRepository.save(apiKey);

        return ApiKeyCreatedResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .keyPrefix(saved.getKeyPrefix())
                .plainTextKey(fullKey)
                .keyType(saved.getKeyType())
                .scopes(saved.getScopes())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public ApiKeyPrincipal validateAndLoad(String apiKey) {
        if (apiKey == null || apiKey.length() < 10) {
            throw new UnauthorizedException("Invalid API key format");
        }
        String prefix = apiKey.substring(0, 10);
        ApiKey key = apiKeyRepository.findByKeyPrefixAndIsActiveTrue(prefix)
                .orElseThrow(() -> new UnauthorizedException("API Key not found or inactive"));

        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("API Key expired");
        }

        if (!passwordEncoder.matches(apiKey, key.getKeyHash())) {
            throw new UnauthorizedException("Invalid API Key");
        }

        // Async update last used (best effort)
        key.setLastUsedAt(Instant.now());
        apiKeyRepository.save(key);

        return ApiKeyPrincipal.builder()
                .organizationId(key.getOrganization().getId())
                .applicationId(key.getApplication() != null ? key.getApplication().getId() : null)
                .keyType(key.getKeyType())
                .scopes(key.getScopes())
                .build();
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    @lombok.Value
    @lombok.Builder
    public static class ApiKeyCreatedResponse {
        UUID id;
        String name;
        String keyPrefix;
        String plainTextKey;
        String keyType;
        List<String> scopes;
        Instant createdAt;
    }

    @lombok.Value
    @lombok.Builder
    public static class ApiKeyPrincipal {
        UUID organizationId;
        UUID applicationId;
        String keyType;
        List<String> scopes;
    }
}
