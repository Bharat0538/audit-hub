package in.audithub.report.service;

import in.audithub.query.dto.AuditEventResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

@Service
public class CsvReportGenerator {

    public byte[] generate(List<AuditEventResponse> events) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter pw = new PrintWriter(baos)) {

            // Header
            pw.println("Event ID,Event Time,Actor User ID,Actor Email,Action Type,Action Name,Resource Type,Resource ID,Outcome,Severity");

            for (AuditEventResponse ev : events) {
                String actorId = ev.getActor() != null ? ev.getActor().getUserId() : "";
                String actorEmail = ev.getActor() != null ? ev.getActor().getUserEmail() : "";
                String actType = ev.getAction() != null && ev.getAction().getType() != null ? ev.getAction().getType().name() : "";
                String actName = ev.getAction() != null ? ev.getAction().getName() : "";
                String resType = ev.getResource() != null ? ev.getResource().getType() : "";
                String resId = ev.getResource() != null ? ev.getResource().getId() : "";
                String outcome = ev.getOutcome() != null ? ev.getOutcome().name() : "";
                String severity = ev.getSeverity() != null ? ev.getSeverity().name() : "";

                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        ev.getEventId(),
                        ev.getEventTime(),
                        escapeCsv(actorId),
                        escapeCsv(actorEmail),
                        escapeCsv(actType),
                        escapeCsv(actName),
                        escapeCsv(resType),
                        escapeCsv(resId),
                        escapeCsv(outcome),
                        escapeCsv(severity));
            }
            pw.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
