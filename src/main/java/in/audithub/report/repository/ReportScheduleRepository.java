package in.audithub.report.repository;

import in.audithub.report.model.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, UUID> {
    List<ReportSchedule> findByOrganizationId(UUID organizationId);
    List<ReportSchedule> findByIsActiveTrue();
    List<ReportSchedule> findByIsActiveTrueAndNextRunAtBefore(Instant time);
}
