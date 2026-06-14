package in.audithub.storage.service;

import in.audithub.tenant.model.Organization;
import in.audithub.tenant.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantRetentionService {

    private final OrganizationRepository organizationRepository;

    public int getRetentionTtlSeconds(UUID orgId) {
        return organizationRepository.findById(orgId)
                .map(org -> {
                    if (org.getRetentionDays() != null && org.getRetentionDays() > 0) {
                        return org.getRetentionDays() * 24 * 60 * 60;
                    }
                    String plan = org.getPlan() != null ? org.getPlan().toUpperCase() : "FREE";
                    return switch (plan) {
                        case "FREE" -> 7 * 24 * 60 * 60;
                        case "STARTER" -> 90 * 24 * 60 * 60;
                        case "PRO" -> 365 * 24 * 60 * 60;
                        case "ENT" -> 7 * 365 * 24 * 60 * 60; // Approximate 7 years
                        default -> 7 * 24 * 60 * 60;
                    };
                })
                .orElse(7 * 24 * 60 * 60);
    }
}
