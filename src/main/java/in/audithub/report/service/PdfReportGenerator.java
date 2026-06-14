package in.audithub.report.service;

import in.audithub.query.dto.AuditEventResponse;
import in.audithub.report.model.GeneratedReport;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfReportGenerator {

    public byte[] generate(GeneratedReport report, List<AuditEventResponse> events) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
                cs.newLineAtOffset(50, 750);
                cs.showText("AUDIT TRAIL REPORT");
                cs.endText();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Report Name: " + report.getName());
                cs.newLineAtOffset(0, -20);
                cs.showText("Format: " + report.getFormat());
                cs.newLineAtOffset(0, -20);
                cs.showText("Status: " + report.getStatus());
                cs.newLineAtOffset(0, -20);
                cs.showText("Total Events: " + events.size());
                cs.newLineAtOffset(0, -20);
                cs.showText("Generated On: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                cs.endText();

                // Draw table header
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                cs.newLineAtOffset(50, 550);
                cs.showText("Event ID | Time | User | Action | Resource | Outcome");
                cs.endText();

                int y = 530;
                for (AuditEventResponse ev : events) {
                    if (y < 50) {
                        break;
                    }
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                    cs.newLineAtOffset(50, y);
                    String userId = ev.getActor() != null ? ev.getActor().getUserId() : "unknown";
                    String actionName = ev.getAction() != null ? ev.getAction().getName() : "unknown";
                    String resourceId = ev.getResource() != null ? ev.getResource().getId() : "unknown";
                    String outcome = ev.getOutcome() != null ? ev.getOutcome().name() : "unknown";
                    String line = String.format("%s | %s | %s | %s | %s | %s",
                            ev.getEventId().toString().substring(0, 8),
                            ev.getEventTime().toString().substring(0, 10),
                            userId, actionName, resourceId, outcome);
                    cs.showText(line);
                    cs.endText();
                    y -= 15;
                }
            }

            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
