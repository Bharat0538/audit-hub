package in.audithub.ingestion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedAuditEvent {
    private UUID eventId;
    private UUID organizationId;
    private UUID applicationId;
    private Instant eventTime;
    private String monthBucket;
    private Actor actor;
    private Action action;
    private Resource resource;
    private List<FieldChange> changes;
    private Outcome outcome;
    private Severity severity;
    private String correlationId;
    private Map<String, String> metadata;
    private List<String> tags;
    private String rawPayload;

    // Enriched fields
    private String geoCountry;
    private String geoCity;
    private Instant ingestedAt;
}
