package in.audithub.report.repository;

import in.audithub.report.model.GeneratedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, UUID> {
    List<GeneratedReport> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
