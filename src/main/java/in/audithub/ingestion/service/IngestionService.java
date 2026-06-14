package in.audithub.ingestion.service;

import in.audithub.ingestion.kafka.AuditEventProducer;
import in.audithub.ingestion.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final QuotaService quotaService;
    private final EventEnricher eventEnricher;
    private final IdempotencyChecker idempotencyChecker;
    private final AuditEventProducer producer;

    public IngestResponse ingest(UUID orgId, UUID appId, AuditEventRequest request) {
        // 1. Quota check (Redis counter, fail fast)
        quotaService.checkAndIncrement(orgId);

        // 2. Resolve application
        UUID resolvedAppId = request.getApplicationId() != null
                ? request.getApplicationId() : appId;

        // 3. Idempotency check
        UUID eventId = request.getEventId() != null
                ? request.getEventId() : UUID.randomUUID();
        if (idempotencyChecker.isDuplicate(orgId, eventId)) {
            return IngestResponse.duplicate(eventId);
        }

        // 4. Enrich
        EnrichedAuditEvent enriched = eventEnricher.enrich(
                orgId, resolvedAppId, eventId, request);

        // 5. Publish to Kafka
        producer.publish(enriched);

        // 6. Mark idempotency key (TTL 24h)
        idempotencyChecker.mark(orgId, eventId);

        return IngestResponse.accepted(eventId);
    }

    @Async
    public CompletableFuture<BatchIngestResponse> ingestBatch(UUID orgId, UUID appId, List<AuditEventRequest> events) {
        int accepted = 0, duplicates = 0, failed = 0;
        List<BatchIngestResponse.Result> results = new ArrayList<>();

        // Batch quota check
        quotaService.checkAndIncrementBatch(orgId, events.size());

        for (int i = 0; i < events.size(); i++) {
            try {
                IngestResponse r = ingest(orgId, appId, events.get(i));
                if (r.getStatus() == IngestStatus.DUPLICATE) duplicates++;
                else accepted++;
                results.add(BatchIngestResponse.Result.success(i, r.getEventId(), r.getStatus()));
            } catch (Exception e) {
                failed++;
                log.error("Batch event {} failed: {}", i, e.getMessage());
                results.add(BatchIngestResponse.Result.failure(i, e.getMessage()));
            }
        }
        return CompletableFuture.completedFuture(
                new BatchIngestResponse(accepted, duplicates, failed, results)
        );
    }
}
