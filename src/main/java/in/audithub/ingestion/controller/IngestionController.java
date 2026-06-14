package in.audithub.ingestion.controller;

import in.audithub.common.security.TenantContext;
import in.audithub.ingestion.model.*;
import in.audithub.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/v1/ingest")
@RequiredArgsConstructor
@Validated
public class IngestionController {

    private final IngestionService ingestionService;
    private final TenantContext tenantContext;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestResponse ingestSingle(@Valid @RequestBody AuditEventRequest request) {
        return ingestionService.ingest(
                tenantContext.getOrganizationId(),
                tenantContext.getApplicationId(),
                request
        );
    }

    @PostMapping("/events/batch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<BatchIngestResponse> ingestBatch(@Valid @RequestBody AuditEventBatchRequest request) {
        return ingestionService.ingestBatch(
                tenantContext.getOrganizationId(),
                tenantContext.getApplicationId(),
                request.getEvents()
        );
    }
}
