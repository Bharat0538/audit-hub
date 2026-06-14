package in.audithub.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.audithub.common.util.MonthBucketUtil;
import in.audithub.ingestion.model.AuditEventRequest;
import in.audithub.ingestion.model.EnrichedAuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventEnricher {

    private final ObjectMapper objectMapper;

    public EnrichedAuditEvent enrich(UUID orgId, UUID appId, UUID eventId, AuditEventRequest req) {
        Instant eventTime = req.getEventTime() != null ? req.getEventTime() : Instant.now();
        String monthBucket = MonthBucketUtil.getMonthBucket(eventTime);
        String rawPayload = toJson(req);

        // Lightweight mock GeoIP enrichment
        String geoCountry = "IN";
        String geoCity = "Mumbai";
        if (req.getActor().getIpAddress() != null && req.getActor().getIpAddress().startsWith("127.")) {
            geoCountry = "LOCALHOST";
            geoCity = "Local";
        }

        return EnrichedAuditEvent.builder()
                .eventId(eventId)
                .organizationId(orgId)
                .applicationId(appId)
                .eventTime(eventTime)
                .monthBucket(monthBucket)
                .actor(req.getActor())
                .action(req.getAction())
                .resource(req.getResource())
                .changes(req.getChanges() != null ? req.getChanges() : List.of())
                .outcome(req.getOutcome())
                .severity(req.getSeverity())
                .correlationId(req.getCorrelationId())
                .metadata(req.getMetadata() != null ? req.getMetadata() : Map.of())
                .tags(req.getTags() != null ? req.getTags() : List.of())
                .rawPayload(rawPayload)
                .geoCountry(geoCountry)
                .geoCity(geoCity)
                .ingestedAt(Instant.now())
                .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
