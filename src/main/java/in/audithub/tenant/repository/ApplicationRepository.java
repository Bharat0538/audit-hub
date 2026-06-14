package in.audithub.tenant.repository;

import in.audithub.tenant.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    Optional<Application> findByOrganizationIdAndSlug(UUID organizationId, String slug);
    List<Application> findByOrganizationId(UUID organizationId);
    long countByOrganizationId(UUID organizationId);
}
