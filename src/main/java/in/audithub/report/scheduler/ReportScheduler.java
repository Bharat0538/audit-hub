package in.audithub.report.scheduler;

import in.audithub.report.model.GeneratedReport;
import in.audithub.report.model.ReportSchedule;
import in.audithub.report.model.ReportTemplate;
import in.audithub.report.repository.GeneratedReportRepository;
import in.audithub.report.repository.ReportScheduleRepository;
import in.audithub.report.repository.ReportTemplateRepository;
import in.audithub.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportScheduler {

    private final ReportScheduleRepository scheduleRepository;
    private final ReportTemplateRepository templateRepository;
    private final GeneratedReportRepository generatedReportRepository;
    private final ReportService reportService;

    @Scheduled(fixedDelay = 10000) // Poll schedules every 10 seconds
    public void runScheduledReports() {
        Instant now = Instant.now();
        List<ReportSchedule> dueSchedules = scheduleRepository.findByIsActiveTrueAndNextRunAtBefore(now);

        for (ReportSchedule schedule : dueSchedules) {
            log.info("Executing scheduled report: {}", schedule.getName());
            ReportTemplate template = templateRepository.findById(schedule.getTemplateId()).orElse(null);
            if (template == null) {
                log.warn("Template {} not found for schedule {}", schedule.getTemplateId(), schedule.getId());
                continue;
            }

            // Create report record
            GeneratedReport report = GeneratedReport.builder()
                    .organizationId(schedule.getOrganizationId())
                    .templateId(schedule.getTemplateId())
                    .name(schedule.getName() + " - " + now)
                    .filters(template.getConfig())
                    .format(template.getFormat())
                    .requestedBy(schedule.getCreatedBy())
                    .status("PENDING")
                    .build();

            GeneratedReport savedReport = generatedReportRepository.save(report);
            reportService.generateReportAsync(savedReport.getId());

            // Update schedule execution times
            schedule.setLastRunAt(now);
            try {
                CronExpression cron = CronExpression.parse(schedule.getCronExpression());
                LocalDateTime next = cron.next(LocalDateTime.now(ZoneId.of(schedule.getTimezone())));
                if (next != null) {
                    schedule.setNextRunAt(next.atZone(ZoneId.of(schedule.getTimezone())).toInstant());
                }
            } catch (Exception e) {
                log.error("Failed to parse cron pattern: {}", schedule.getCronExpression(), e);
                schedule.setActive(false); // Disable failing schedules
            }
            scheduleRepository.save(schedule);
        }
    }
}
