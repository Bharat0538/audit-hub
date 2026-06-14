package in.audithub.alert.repository;

import in.audithub.alert.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {
    List<AlertRule> findByOrganizationId(UUID organizationId);
    List<AlertRule> findByOrganizationIdAndIsActiveTrue(UUID organizationId);
}
