package in.audithub.report.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.audithub.common.model.ApiResponse;
import in.audithub.common.security.TenantContext;
import in.audithub.report.model.GeneratedReport;
import in.audithub.report.model.ReportSchedule;
import in.audithub.report.model.ReportTemplate;
import in.audithub.report.repository.GeneratedReportRepository;
import in.audithub.report.repository.ReportScheduleRepository;
import in.audithub.report.repository.ReportTemplateRepository;
import in.audithub.report.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportTemplateRepository templateRepository;
    private final GeneratedReportRepository generatedReportRepository;
    private final ReportScheduleRepository scheduleRepository;
    private final ReportService reportService;
    private final TenantContext tenantContext;
    private final ObjectMapper objectMapper;

    // ─── TEMPLATES ───

    @PostMapping("/templates")
    public ApiResponse<ReportTemplate> createTemplate(@RequestBody ReportTemplate template) {
        UUID orgId = tenantContext.getOrganizationId();
        template.setOrganizationId(orgId);
        ReportTemplate saved = templateRepository.save(template);
        return ApiResponse.success(saved);
    }

    @GetMapping("/templates")
    public ApiResponse<List<ReportTemplate>> getTemplates() {
        UUID orgId = tenantContext.getOrganizationId();
        List<ReportTemplate> templates = templateRepository.findByOrganizationIdOrIsSystemTrue(orgId);
        return ApiResponse.success(templates);
    }

    // ─── REPORTS ───

    @PostMapping
    public ApiResponse<GeneratedReport> generateReport(@RequestBody GeneratedReport report) {
        UUID orgId = tenantContext.getOrganizationId();
        report.setOrganizationId(orgId);
        report.setRequestedBy(UUID.randomUUID());
        report.setStatus("PENDING");
        GeneratedReport saved = generatedReportRepository.save(report);

        reportService.generateReportAsync(saved.getId());

        return ApiResponse.success(saved);
    }

    @GetMapping
    public ApiResponse<List<GeneratedReport>> getReports() {
        UUID orgId = tenantContext.getOrganizationId();
        List<GeneratedReport> reports = generatedReportRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        return ApiResponse.success(reports);
    }

    @GetMapping("/{id}")
    public ApiResponse<GeneratedReport> getReportDetails(@PathVariable UUID id) {
        GeneratedReport report = generatedReportRepository.findById(id).orElse(null);
        return ApiResponse.success(report);
    }

    // ─── SCHEDULES ───

    @PostMapping("/schedules")
    public ApiResponse<ReportSchedule> createSchedule(@RequestBody ReportSchedule schedule) {
        UUID orgId = tenantContext.getOrganizationId();
        schedule.setOrganizationId(orgId);
        schedule.setCreatedBy(UUID.randomUUID());
        schedule.setActive(true);

        try {
            CronExpression cron = CronExpression.parse(schedule.getCronExpression());
            LocalDateTime next = cron.next(LocalDateTime.now(ZoneId.of(schedule.getTimezone())));
            if (next != null) {
                schedule.setNextRunAt(next.atZone(ZoneId.of(schedule.getTimezone())).toInstant());
            }
        } catch (Exception e) {
            log.error("Failed to parse schedule cron expression", e);
        }

        ReportSchedule saved = scheduleRepository.save(schedule);
        return ApiResponse.success(saved);
    }

    // ─── DOWNLOAD FILE ───

    @GetMapping("/download/{id}")
    public void downloadReport(@PathVariable UUID id, HttpServletResponse response) {
        GeneratedReport report = generatedReportRepository.findById(id).orElse(null);
        if (report == null || !"COMPLETED".equals(report.getStatus())) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        File file = new File(report.getS3Key());
        if (!file.exists()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        String mimeType = "PDF".equalsIgnoreCase(report.getFormat()) ? "application/pdf" : "text/csv";
        response.setContentType(mimeType);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
        response.setContentLength((int) file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (Exception e) {
            log.error("Failed to stream report file download", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
