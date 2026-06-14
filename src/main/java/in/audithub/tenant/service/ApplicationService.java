package in.audithub.tenant.service;

import in.audithub.tenant.model.Application;
import in.audithub.tenant.model.Organization;
import in.audithub.tenant.repository.ApplicationRepository;
import in.audithub.tenant.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final OrganizationRepository organizationRepository;

    public Application create(String name, String environment, String description, String webhookUrl, UUID organizationId, UUID createdBy) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        long count = applicationRepository.countByOrganizationId(organizationId);
        if (count >= org.getMaxApplications()) {
            throw new IllegalArgumentException("Maximum applications limit reached. Upgrade your plan.");
        }

        String slug = slugify(name);
        if (applicationRepository.findByOrganizationIdAndSlug(organizationId, slug).isPresent()) {
            throw new IllegalArgumentException("Application slug already exists in organization");
        }

        Application app = Application.builder()
                .organization(org)
                .name(name)
                .slug(slug)
                .description(description)
                .environment(environment)
                .status("ACTIVE")
                .webhookUrl(webhookUrl)
                .webhookSecret("test-webhook-secret")
                .createdBy(createdBy)
                .build();

        return applicationRepository.save(app);
    }

    public List<Application> getByOrganization(UUID organizationId) {
        return applicationRepository.findByOrganizationId(organizationId);
    }

    private String slugify(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .trim();
    }
}
