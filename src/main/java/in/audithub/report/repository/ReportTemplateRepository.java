package in.audithub.report.repository;

import in.audithub.report.model.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, UUID> {
    List<ReportTemplate> findByOrganizationIdOrIsSystemTrue(UUID organizationId);
}
