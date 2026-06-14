package in.audithub.storage.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.audithub.ingestion.model.EnrichedAuditEvent;
import in.audithub.storage.repository.AuditEventRepository;
import in.audithub.storage.service.AlertEvaluator;
import in.audithub.storage.service.DailyStatsUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventStorageConsumer {

    private final AuditEventRepository repository;
    private final DailyStatsUpdater statsUpdater;
    private final AlertEvaluator alertEvaluator;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "audit.events.enriched",
            groupId = "audithub-storage-service",
            concurrency = "8",
            containerFactory = "auditKafkaListenerContainerFactory"
    )
    public void consume(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.debug("Consuming event from partition {} offset {}", partition, offset);
        try {
            EnrichedAuditEvent event = deserialize(payload);

            // Write to all Cassandra tables in parallel (fan-out)
            CompletableFuture.allOf(
                    repository.saveToMainTable(event),
                    repository.saveToUserTable(event),
                    repository.saveToResourceTable(event),
                    repository.saveToSeverityTable(event),
                    repository.saveToEntityTimeline(event)
            ).join();

            // Update daily stats counters (async, best-effort)
            statsUpdater.increment(event);

            // Evaluate alert rules (async)
            alertEvaluator.evaluate(event);

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Storage failed for partition {} offset {}: {}",
                    partition, offset, e.getMessage(), e);
        }
    }

    private EnrichedAuditEvent deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, EnrichedAuditEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
}
