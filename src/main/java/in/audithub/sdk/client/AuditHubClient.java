package in.audithub.sdk.client;

import in.audithub.ingestion.model.AuditEventBatchRequest;
import in.audithub.ingestion.model.AuditEventRequest;
import in.audithub.sdk.config.AuditHubProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class AuditHubClient {

    private final AuditHubProperties props;
    private RestClient restClient;
    private final BlockingQueue<AuditEventRequest> queue;
    private final ScheduledExecutorService scheduler;

    public AuditHubClient(AuditHubProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("X-API-Key", props.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.queue = new ArrayBlockingQueue<>(10000);
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "audithub-sdk-flusher");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void updateConfig(String apiKey, java.util.UUID applicationId) {
        props.setApiKey(apiKey);
        props.setApplicationId(applicationId);
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("X-API-Key", apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("AuditHub Client SDK configuration updated (applicationId: {})", applicationId);
    }

    @PostConstruct
    public void init() {
        if (props.isEnabled() && props.isAsync()) {
            scheduler.scheduleAtFixedRate(this::flush, props.getFlushIntervalMs(), props.getFlushIntervalMs(), TimeUnit.MILLISECONDS);
            log.info("AuditHub Client SDK async flusher started (interval: {}ms, batchSize: {})", 
                    props.getFlushIntervalMs(), props.getBatchSize());
        }
    }

    public void send(AuditEventRequest event) {
        if (!props.isEnabled()) {
            return;
        }

        // Set application ID from config if not present
        if (event.getApplicationId() == null && props.getApplicationId() != null) {
            event.setApplicationId(props.getApplicationId());
        }

        if (props.isAsync()) {
            boolean offered = queue.offer(event);
            if (!offered) {
                log.warn("AuditHub SDK local queue is full - dropping event for resource {}", 
                        event.getResource() != null ? event.getResource().getId() : "unknown");
            }
        } else {
            sendImmediateWithRetry(event);
        }
    }

    private void flush() {
        try {
            List<AuditEventRequest> batch = new ArrayList<>();
            queue.drainTo(batch, props.getBatchSize());

            if (batch.isEmpty()) {
                return;
            }

            sendBatchWithRetry(batch);
        } catch (Exception e) {
            log.error("Error during AuditHub SDK background flush: {}", e.getMessage(), e);
        }
    }

    private void sendImmediateWithRetry(AuditEventRequest event) {
        int attempt = 0;
        long backoffMs = 1000;
        while (attempt < props.getRetryMaxAttempts()) {
            try {
                restClient.post()
                        .uri("/v1/ingest/events")
                        .body(event)
                        .retrieve()
                        .toBodilessEntity();
                return; // Success
            } catch (Exception e) {
                attempt++;
                if (attempt >= props.getRetryMaxAttempts()) {
                    log.error("Failed to send event to AuditHub after {} attempts: {}", attempt, e.getMessage());
                } else {
                    log.warn("Failed to send event to AuditHub (attempt {}/{}), retrying in {}ms... Error: {}", 
                            attempt, props.getRetryMaxAttempts(), backoffMs, e.getMessage());
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    backoffMs *= 2; // Exponential backoff
                }
            }
        }
    }

    private void sendBatchWithRetry(List<AuditEventRequest> batch) {
        AuditEventBatchRequest batchReq = new AuditEventBatchRequest();
        batchReq.setEvents(batch);
        int attempt = 0;
        long backoffMs = 1000;
        while (attempt < props.getRetryMaxAttempts()) {
            try {
                restClient.post()
                        .uri("/v1/ingest/events/batch")
                        .body(batchReq)
                        .retrieve()
                        .toBodilessEntity();
                return; // Success
            } catch (Exception e) {
                attempt++;
                if (attempt >= props.getRetryMaxAttempts()) {
                    log.error("Failed to send batch of {} events to AuditHub after {} attempts. Events dropped.", 
                            batch.size(), attempt);
                } else {
                    log.warn("Failed to send batch to AuditHub (attempt {}/{}), retrying in {}ms... Error: {}", 
                            attempt, props.getRetryMaxAttempts(), backoffMs, e.getMessage());
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    backoffMs *= 2; // Exponential backoff
                }
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down AuditHub Client SDK... Flushing remaining events.");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        // Final drain
        List<AuditEventRequest> remaining = new ArrayList<>();
        queue.drainTo(remaining);
        if (!remaining.isEmpty()) {
            try {
                // Perform one final synchronous flush (best-effort)
                sendBatchWithRetry(remaining);
            } catch (Exception e) {
                log.error("Final flush failed: {}", e.getMessage());
            }
        }
    }
}
