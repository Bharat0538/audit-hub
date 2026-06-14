package in.audithub.alert.dto;

import in.audithub.ingestion.model.EnrichedAuditEvent;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class WebhookPayload {
    private UUID alertRuleId;
    private String alertRuleName;
    private String severity;
    private long triggerCount;
    private String groupByValue;
    private Instant timestamp;
    private EnrichedAuditEvent triggeringEvent;
}
