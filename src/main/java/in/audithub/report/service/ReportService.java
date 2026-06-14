package in.audithub.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.audithub.query.dto.AuditEventPage;
import in.audithub.query.dto.AuditEventResponse;
import in.audithub.query.dto.AuditEventSearchRequest;
import in.audithub.storage.repository.AuditEventRepository;
import in.audithub.report.model.GeneratedReport;
import in.audithub.report.repository.GeneratedReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final AuditEventRepository auditEventRepository;
    private final GeneratedReportRepository reportRepository;
    private final PdfReportGenerator pdfGenerator;
    private final CsvReportGenerator csvGenerator;
    private final ObjectMapper objectMapper;

    @Async
    public void generateReportAsync(UUID reportId) {
        GeneratedReport report = reportRepository.findById(reportId).orElse(null);
        if (report == null) return;

        report.setStatus("PROCESSING");
        report.setStartedAt(Instant.now());
        reportRepository.save(report);

        try {
            AuditEventSearchRequest searchReq = objectMapper.readValue(report.getFilters(), AuditEventSearchRequest.class);
            searchReq.setOrganizationId(report.getOrganizationId());

            List<AuditEventResponse> allEvents = new ArrayList<>();
            String pageToken = null;
            do {
                searchReq.setPageToken(pageToken);
                AuditEventPage<AuditEventResponse> page = auditEventRepository.search(searchReq);
                allEvents.addAll(page.getContent());
                pageToken = page.getPageToken();
            } while (pageToken != null);

            byte[] reportBytes;
            String fileExtension;
            report.setStatus("COMPLETED");
            if ("PDF".equalsIgnoreCase(report.getFormat())) {
                reportBytes = pdfGenerator.generate(report, allEvents);
                fileExtension = "pdf";
            } else {
                reportBytes = csvGenerator.generate(allEvents);
                fileExtension = "csv";
            }

            File reportsDir = new File("target/reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }

            File reportFile = new File(reportsDir, report.getId() + "." + fileExtension);
            try (FileOutputStream fos = new FileOutputStream(reportFile)) {
                fos.write(reportBytes);
            }

            report.setStatus("COMPLETED");
            report.setCompletedAt(Instant.now());
            report.setRowCount((long) allEvents.size());
            report.setFileSizeBytes((long) reportBytes.length);
            report.setS3Key(reportFile.getAbsolutePath());
            report.setPresignedUrl("http://localhost:8080/v1/reports/download/" + report.getId());
            report.setPresignedUrlExpiresAt(Instant.now().plusSeconds(3600)); // 1 hour
            reportRepository.save(report);

        } catch (Exception e) {
            log.error("Report generation failed for ID: {}", reportId, e);
            report.setStatus("FAILED");
            report.setErrorMessage(e.getMessage());
            report.setCompletedAt(Instant.now());
            reportRepository.save(report);
        }
    }
}
