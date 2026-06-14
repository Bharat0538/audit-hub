package in.audithub.tenant.repository;

import in.audithub.tenant.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyPrefixAndIsActiveTrue(String keyPrefix);
    java.util.List<ApiKey> findByOrganizationId(UUID organizationId);
    long countByApplicationIdAndIsActive(UUID applicationId, boolean isActive);
}
