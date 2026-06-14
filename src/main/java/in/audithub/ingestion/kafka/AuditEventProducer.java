package in.audithub.ingestion.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.audithub.common.config.KafkaConfig;
import in.audithub.ingestion.model.EnrichedAuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(EnrichedAuditEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String partitionKey = event.getOrganizationId().toString();

            kafkaTemplate.send(KafkaConfig.AUDIT_ENRICHED, partitionKey, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event {}: {}", event.getEventId(), ex.getMessage());
                            publishToDlq(event, ex.getMessage());
                        } else {
                            log.debug("Event {} published to partition {}",
                                    event.getEventId(),
                                    result.getRecordMetadata().partition());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event", e);
        }
    }

    private void publishToDlq(EnrichedAuditEvent event, String errorReason) {
        try {
            // Simple DLQ publish
            String partitionKey = event.getOrganizationId().toString();
            kafkaTemplate.send(KafkaConfig.AUDIT_DLQ, partitionKey, "Error: " + errorReason + ", Event: " + objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to publish to DLQ: {}", e.getMessage());
        }
    }
}
