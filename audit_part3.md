
---

## 8. Backend Microservices Design

### 8.1 Ingestion Service

**Responsibility:** Accept audit events via REST and Kafka, validate, enrich, and publish to the internal Kafka pipeline.

```java
// ─────────────────────────────────────────────────────────────────
// Domain Model
// ─────────────────────────────────────────────────────────────────

@Value
@Builder
@JsonDeserialize(builder = AuditEventRequest.AuditEventRequestBuilder.class)
public class AuditEventRequest {
    @Nullable UUID eventId;
    @Nullable UUID applicationId;
    @Nullable Instant eventTime;
    @NotNull Actor actor;
    @NotNull Action action;
    @NotNull Resource resource;
    @Nullable List<FieldChange> changes;
    @Builder.Default Outcome outcome = Outcome.SUCCESS;
    @Builder.Default Severity severity = Severity.LOW;
    @Nullable String correlationId;
    @Nullable Map<String, String> metadata;
    @Nullable List<String> tags;
}

@Value
@Builder
public class EnrichedAuditEvent {
    UUID eventId;
    UUID organizationId;
    UUID applicationId;
    Instant eventTime;
    String monthBucket;           // "2025-06" — derived
    Actor actor;
    Action action;
    Resource resource;
    List<FieldChange> changes;
    Outcome outcome;
    Severity severity;
    String correlationId;
    Map<String, String> metadata;
    List<String> tags;
    String rawPayload;            // original JSON string
    // Enriched fields
    String geoCountry;
    String geoCity;
    Instant ingestedAt;
}

// ─────────────────────────────────────────────────────────────────
// Controller
// ─────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/v1/ingest")
@RequiredArgsConstructor
@Validated
public class IngestionController {

    private final IngestionService ingestionService;
    private final TenantContext tenantContext;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestResponse ingestSingle(
            @Valid @RequestBody AuditEventRequest request) {
        return ingestionService.ingest(tenantContext.getOrganizationId(),
                tenantContext.getApplicationId(), request);
    }

    @PostMapping("/events/batch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BatchIngestResponse ingestBatch(
            @Valid @RequestBody @Size(max = 1000) AuditEventBatchRequest request) {
        return ingestionService.ingestBatch(tenantContext.getOrganizationId(),
                tenantContext.getApplicationId(), request.getEvents());
    }
}

// ─────────────────────────────────────────────────────────────────
// Service
// ─────────────────────────────────────────────────────────────────

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final QuotaService quotaService;
    private final EventEnricher eventEnricher;
    private final IdempotencyChecker idempotencyChecker;
    private final AuditEventProducer producer;
    private final MeterRegistry meterRegistry;

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

        // 7. Metrics
        meterRegistry.counter("audit.events.ingested",
                "org", orgId.toString(),
                "severity", request.getSeverity().name()).increment();

        return IngestResponse.accepted(eventId);
    }

    @Async("batchExecutor")
    public BatchIngestResponse ingestBatch(UUID orgId, UUID appId,
                                            List<AuditEventRequest> events) {
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
        return new BatchIngestResponse(accepted, duplicates, failed, results);
    }
}

// ─────────────────────────────────────────────────────────────────
// Event Enricher
// ─────────────────────────────────────────────────────────────────

@Service
@RequiredArgsConstructor
public class EventEnricher {

    private final GeoIpService geoIpService;
    private final ObjectMapper objectMapper;

    public EnrichedAuditEvent enrich(UUID orgId, UUID appId,
                                      UUID eventId, AuditEventRequest req) {
        String rawPayload = toJson(req);
        String monthBucket = YearMonth.from(
                (req.getEventTime() != null ? req.getEventTime() : Instant.now())
                        .atZone(ZoneId.of("Asia/Kolkata"))).toString();

        GeoLocation geo = Optional.ofNullable(req.getActor().getIpAddress())
                .map(geoIpService::lookup)
                .orElse(GeoLocation.UNKNOWN);

        return EnrichedAuditEvent.builder()
                .eventId(eventId)
                .organizationId(orgId)
                .applicationId(appId)
                .eventTime(req.getEventTime() != null ? req.getEventTime() : Instant.now())
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
                .geoCountry(geo.getCountry())
                .geoCity(geo.getCity())
                .ingestedAt(Instant.now())
                .build();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}

// ─────────────────────────────────────────────────────────────────
// Quota Service — Redis-backed
// ─────────────────────────────────────────────────────────────────

@Service
@RequiredArgsConstructor
public class QuotaService {

    private final StringRedisTemplate redis;
    private final OrganizationCacheService orgCache;

    private static final String QUOTA_KEY = "quota:monthly:%s:%s"; // orgId:yearMonth

    public void checkAndIncrement(UUID orgId) {
        Organization org = orgCache.get(orgId);
        String key = quotaKey(orgId);
        Long current = redis.opsForValue().increment(key);
        if (current == 1) {
            // Set expiry on first increment of the month
            redis.expire(key, Duration.ofDays(35));
        }
        if (current > org.getMaxEventsPerMonth()) {
            throw new QuotaExceededException(
                    "Monthly event quota of " + org.getMaxEventsPerMonth() + " exceeded");
        }
    }

    public void checkAndIncrementBatch(UUID orgId, int count) {
        Organization org = orgCache.get(orgId);
        String key = quotaKey(orgId);
        Long current = redis.opsForValue().increment(key, count);
        if (current == count) {
            redis.expire(key, Duration.ofDays(35));
        }
        if (current > org.getMaxEventsPerMonth()) {
            // Rollback the batch increment
            redis.opsForValue().decrement(key, count);
            throw new QuotaExceededException("Monthly event quota exceeded");
        }
    }

    public long getCurrentUsage(UUID orgId) {
        String val = redis.opsForValue().get(quotaKey(orgId));
        return val != null ? Long.parseLong(val) : 0L;
    }

    private String quotaKey(UUID orgId) {
        String ym = YearMonth.now(ZoneId.of("Asia/Kolkata")).toString();
        return String.format(QUOTA_KEY, orgId, ym);
    }
}
```

### 8.2 Kafka Producer & Consumer

```java
// ─────────────────────────────────────────────────────────────────
// Kafka Topics Constants
// ─────────────────────────────────────────────────────────────────

public final class KafkaTopics {
    public static final String AUDIT_RAW       = "audit.events.raw";
    public static final String AUDIT_ENRICHED  = "audit.events.enriched";
    public static final String AUDIT_DLQ       = "audit.events.dlq";
    public static final String AUDIT_REPLAY    = "audit.events.replay";
    public static final String AUDIT_ALERTS    = "audit.alerts";
    private KafkaTopics() {}
}

// ─────────────────────────────────────────────────────────────────
// Producer
// ─────────────────────────────────────────────────────────────────

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Partition key = organizationId — ensures all events of one tenant
     * go to same partition, maintaining order within tenant.
     */
    public void publish(EnrichedAuditEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String partitionKey = event.getOrganizationId().toString();

            kafkaTemplate.send(KafkaTopics.AUDIT_ENRICHED, partitionKey, payload)
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
            throw new IngestionException("Failed to serialize event", e);
        }
    }

    private void publishToDlq(EnrichedAuditEvent event, String errorReason) {
        try {
            DlqMessage dlq = DlqMessage.of(event, errorReason, Instant.now());
            kafkaTemplate.send(KafkaTopics.AUDIT_DLQ,
                    event.getOrganizationId().toString(),
                    objectMapper.writeValueAsString(dlq));
        } catch (Exception e) {
            log.error("Failed to publish to DLQ: {}", e.getMessage());
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Storage Service — Cassandra Consumer
// ─────────────────────────────────────────────────────────────────

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventStorageConsumer {

    private final AuditEventRepository repository;
    private final DailyStatsUpdater statsUpdater;
    private final AlertEvaluator alertEvaluator;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = KafkaTopics.AUDIT_ENRICHED,
            groupId = "audithub-storage-service",
            concurrency = "8",  // 8 threads, one per partition (tune per deployment)
            containerFactory = "auditKafkaListenerContainerFactory"
    )
    public void consume(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        Timer.Sample sample = Timer.start(meterRegistry);
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
            meterRegistry.counter("audit.events.stored",
                    "org", event.getOrganizationId().toString()).increment();

        } catch (Exception e) {
            log.error("Storage failed for partition {} offset {}: {}",
                    partition, offset, e.getMessage(), e);
            // Don't ack — Kafka will retry
            // After max retries, goes to DLQ via error handler
        } finally {
            sample.stop(meterRegistry.timer("audit.storage.duration"));
        }
    }

    private EnrichedAuditEvent deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, EnrichedAuditEvent.class);
        } catch (JsonProcessingException e) {
            throw new StorageException("Failed to deserialize event", e);
        }
    }
}
```

### 8.3 Cassandra Repository

```java
@Repository
@RequiredArgsConstructor
public class AuditEventRepository {

    private final CassandraOperations cassandraOps;
    private final TenantRetentionService retentionService;

    /**
     * Write to audit_events main table.
     * TTL is set per-tenant based on their subscription plan.
     */
    public CompletableFuture<Void> saveToMainTable(EnrichedAuditEvent event) {
        int ttlSeconds = retentionService.getRetentionTtlSeconds(event.getOrganizationId());

        SimpleStatement stmt = QueryBuilder.insertInto("audit_events")
                .value("organization_id",   literal(event.getOrganizationId()))
                .value("application_id",    literal(event.getApplicationId()))
                .value("month_bucket",      literal(event.getMonthBucket()))
                .value("event_time",        literal(event.getEventTime()))
                .value("event_id",          literal(event.getEventId()))
                .value("actor_user_id",     literal(event.getActor().getUserId()))
                .value("actor_user_email",  literal(event.getActor().getUserEmail()))
                .value("actor_user_name",   literal(event.getActor().getUserName()))
                .value("actor_ip_address",  literal(event.getActor().getIpAddress()))
                .value("action_type",       literal(event.getAction().getType().name()))
                .value("action_name",       literal(event.getAction().getName()))
                .value("action_description",literal(event.getAction().getDescription()))
                .value("resource_type",     literal(event.getResource().getType()))
                .value("resource_id",       literal(event.getResource().getId()))
                .value("resource_name",     literal(event.getResource().getName()))
                .value("changes",           literal(serializeChanges(event.getChanges())))
                .value("outcome",           literal(event.getOutcome().name()))
                .value("severity",          literal(event.getSeverity().name()))
                .value("metadata",          literal(event.getMetadata()))
                .value("correlation_id",    literal(event.getCorrelationId()))
                .value("tags",              literal(event.getTags()))
                .value("raw_payload",       literal(event.getRawPayload()))
                .usingTtl(ttlSeconds)
                .build();

        return CompletableFuture.runAsync(() ->
                cassandraOps.getCqlOperations().execute(stmt));
    }

    public CompletableFuture<Void> saveToUserTable(EnrichedAuditEvent event) {
        int ttlSeconds = retentionService.getRetentionTtlSeconds(event.getOrganizationId());

        SimpleStatement stmt = QueryBuilder.insertInto("audit_events_by_user")
                .value("organization_id",   literal(event.getOrganizationId()))
                .value("actor_user_id",     literal(event.getActor().getUserId()))
                .value("month_bucket",      literal(event.getMonthBucket()))
                .value("event_time",        literal(event.getEventTime()))
                .value("event_id",          literal(event.getEventId()))
                .value("application_id",    literal(event.getApplicationId()))
                .value("action_type",       literal(event.getAction().getType().name()))
                .value("action_name",       literal(event.getAction().getName()))
                .value("resource_type",     literal(event.getResource().getType()))
                .value("resource_id",       literal(event.getResource().getId()))
                .value("resource_name",     literal(event.getResource().getName()))
                .value("outcome",           literal(event.getOutcome().name()))
                .value("severity",          literal(event.getSeverity().name()))
                .value("actor_ip_address",  literal(event.getActor().getIpAddress()))
                .usingTtl(ttlSeconds)
                .build();

        return CompletableFuture.runAsync(() ->
                cassandraOps.getCqlOperations().execute(stmt));
    }

    public CompletableFuture<Void> saveToResourceTable(EnrichedAuditEvent event) {
        int ttlSeconds = retentionService.getRetentionTtlSeconds(event.getOrganizationId());

        SimpleStatement stmt = QueryBuilder.insertInto("audit_events_by_resource")
                .value("organization_id",   literal(event.getOrganizationId()))
                .value("resource_type",     literal(event.getResource().getType()))
                .value("resource_id",       literal(event.getResource().getId()))
                .value("event_time",        literal(event.getEventTime()))
                .value("event_id",          literal(event.getEventId()))
                .value("application_id",    literal(event.getApplicationId()))
                .value("actor_user_id",     literal(event.getActor().getUserId()))
                .value("actor_user_email",  literal(event.getActor().getUserEmail()))
                .value("action_type",       literal(event.getAction().getType().name()))
                .value("action_name",       literal(event.getAction().getName()))
                .value("changes",           literal(serializeChanges(event.getChanges())))
                .value("outcome",           literal(event.getOutcome().name()))
                .value("severity",          literal(event.getSeverity().name()))
                .usingTtl(ttlSeconds)
                .build();

        return CompletableFuture.runAsync(() ->
                cassandraOps.getCqlOperations().execute(stmt));
    }

    /**
     * Query: Search events by org + app + time range
     * Paginated using Cassandra PagingState
     */
    public AuditEventPage search(AuditEventSearchRequest req) {
        List<String> monthBuckets = generateMonthBuckets(req.getStartTime(), req.getEndTime());

        // Cassandra doesn't support cross-partition queries efficiently
        // We query each month bucket and merge results
        List<AuditEvent> allResults = new ArrayList<>();
        PagingState pagingState = decodePagingState(req.getPageToken());

        for (String bucket : monthBuckets) {
            Select select = QueryBuilder.selectFrom("audit_events")
                    .all()
                    .whereColumn("organization_id").isEqualTo(literal(req.getOrganizationId()))
                    .whereColumn("application_id").isEqualTo(literal(req.getApplicationId()))
                    .whereColumn("month_bucket").isEqualTo(literal(bucket))
                    .whereColumn("event_time").isGreaterThanOrEqualTo(literal(req.getStartTime()))
                    .whereColumn("event_time").isLessThanOrEqualTo(literal(req.getEndTime()))
                    .limit(req.getPageSize());

            // Apply optional filters in-memory (Cassandra can't filter clustering key mid-range)
            // For production, Cassandra Materialized Views or Lucene index (DSE) handles this
            SimpleStatement stmt = select.build().setPageSize(req.getPageSize());
            if (pagingState != null) {
                stmt = stmt.setPagingState(pagingState);
                pagingState = null; // Only apply to first bucket
            }

            ResultSet rs = cassandraOps.getCqlOperations().queryForResultSet(stmt);
            rs.forEach(row -> allResults.add(mapRow(row)));

            if (allResults.size() >= req.getPageSize()) {
                PagingState nextState = rs.getExecutionInfo().getPagingState();
                return AuditEventPage.of(
                        allResults.subList(0, req.getPageSize()),
                        encodePagingState(nextState, bucket),
                        nextState != null);
            }
        }

        return AuditEventPage.of(allResults, null, false);
    }

    private List<String> generateMonthBuckets(Instant start, Instant end) {
        List<String> buckets = new ArrayList<>();
        YearMonth startMonth = YearMonth.from(start.atZone(ZoneId.of("Asia/Kolkata")));
        YearMonth endMonth = YearMonth.from(end.atZone(ZoneId.of("Asia/Kolkata")));
        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            buckets.add(current.toString());
            current = current.plusMonths(1);
        }
        return buckets;
    }
}
```

### 8.4 Query Service

```java
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditEventRepository repository;
    private final RedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public AuditEventPage searchEvents(AuditEventSearchRequest req) {
        // Cache only when filters are simple and result set is small
        // Don't cache paginated results beyond page 1
        if (req.getPageToken() == null && isCacheable(req)) {
            String cacheKey = buildCacheKey(req);
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                return deserializePage(cached);
            }
            AuditEventPage result = repository.search(req);
            redis.opsForValue().set(cacheKey, serializePage(result), CACHE_TTL);
            return result;
        }
        return repository.search(req);
    }

    public AuditEventPage getEntityHistory(UUID orgId, String resourceType,
                                            String resourceId, Instant start,
                                            Instant end, String pageToken, int pageSize) {
        return repository.searchByResource(orgId, resourceType, resourceId,
                start, end, pageToken, pageSize);
    }

    public AuditEventPage getUserActivity(UUID orgId, String userId,
                                           Instant start, Instant end,
                                           String pageToken, int pageSize) {
        return repository.searchByUser(orgId, userId, start, end, pageToken, pageSize);
    }

    private boolean isCacheable(AuditEventSearchRequest req) {
        // Only cache simple time-range queries without fine-grained filters
        return req.getActorUserId() == null
                && req.getResourceId() == null
                && (req.getActionTypes() == null || req.getActionTypes().isEmpty());
    }

    private String buildCacheKey(AuditEventSearchRequest req) {
        return String.format("query:%s:%s:%s:%s:%d",
                req.getOrganizationId(), req.getApplicationId(),
                req.getStartTime().truncatedTo(ChronoUnit.HOURS),
                req.getEndTime().truncatedTo(ChronoUnit.HOURS),
                req.getPageSize());
    }
}
```

### 8.5 Report Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final AuditEventRepository repository;
    private final PdfReportGenerator pdfGenerator;
    private final CsvReportGenerator csvGenerator;
    private final S3Service s3Service;
    private final GeneratedReportRepository reportRepository;

    /**
     * Async report generation — triggered by REST call,
     * executed in background thread pool.
     */
    @Async("reportExecutor")
    public CompletableFuture<GeneratedReport> generateReport(
            UUID orgId, UUID userId, GenerateReportRequest req) {

        GeneratedReport report = reportRepository.save(GeneratedReport.builder()
                .organizationId(orgId)
                .name(req.getName())
                .format(req.getFormat())
                .filters(req.getFilters())
                .status(ReportStatus.PROCESSING)
                .requestedBy(userId)
                .startedAt(Instant.now())
                .build());

        try {
            // Stream events from Cassandra to avoid OOM
            List<AuditEventResponse> events = streamAllEvents(orgId, req.getFilters());

            byte[] reportBytes = switch (req.getFormat()) {
                case PDF  -> pdfGenerator.generate(report, events, orgId);
                case CSV  -> csvGenerator.generate(report, events);
                case XLSX -> csvGenerator.generateXlsx(report, events);
            };

            // Upload to S3
            String s3Key = String.format("reports/%s/%s/%s.%s",
                    orgId, YearMonth.now(), report.getId(),
                    req.getFormat().name().toLowerCase());
            s3Service.upload(s3Key, reportBytes, req.getFormat().getContentType());

            // Generate presigned URL (1 hour)
            String presignedUrl = s3Service.generatePresignedUrl(s3Key, Duration.ofHours(1));

            reportRepository.markCompleted(report.getId(), s3Key,
                    presignedUrl, (long) events.size(), (long) reportBytes.length);

            return CompletableFuture.completedFuture(
                    reportRepository.findById(report.getId()).orElseThrow());

        } catch (Exception e) {
            log.error("Report generation failed for {}: {}", report.getId(), e.getMessage(), e);
            reportRepository.markFailed(report.getId(), e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// PDF Generator using Apache PDFBox
// ─────────────────────────────────────────────────────────────────

@Service
public class PdfReportGenerator {

    public byte[] generate(GeneratedReport report,
                            List<AuditEventResponse> events, UUID orgId) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            addCoverPage(doc, report, orgId);
            addSummaryPage(doc, events);
            addEventTable(doc, events);
            addFooter(doc);

            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private void addCoverPage(PDDocument doc, GeneratedReport report, UUID orgId) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // AuditHub branding
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 24);
            cs.newLineAtOffset(50, 750);
            cs.showText("AUDIT TRAIL REPORT");
            cs.endText();

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(50, 700);
            cs.showText("Report: " + report.getName());
            cs.newLine();
            cs.showText("Generated: " + LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss IST")));
            cs.newLine();
            cs.showText("Organization: " + orgId.toString());
            cs.newLine();
            cs.showText("Total Events: " + events.size());
            cs.endText();
        }
    }
}
```

### 8.6 Auth & Security Filter Chain

```java
// ─────────────────────────────────────────────────────────────────
// Security Config
// ─────────────────────────────────────────────────────────────────

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ApiKeyAuthenticationFilter apiKeyFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/auth/**").permitAll()
                        .requestMatchers("/v1/organizations").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/v1/ingest/**").hasAuthority("SCOPE_audit:write")
                        .requestMatchers(HttpMethod.GET, "/v1/events/**").hasAnyRole("ADMIN", "AUDITOR", "VIEWER")
                        .requestMatchers("/v1/reports/**").hasAnyRole("ADMIN", "AUDITOR")
                        .requestMatchers("/v1/replay/**").hasRole("ADMIN")
                        .requestMatchers("/v1/organizations/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

// ─────────────────────────────────────────────────────────────────
// API Key Filter
// ─────────────────────────────────────────────────────────────────

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final StringRedisTemplate redis;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || !apiKey.startsWith("ah_")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // Cache validated API keys for 5 minutes to reduce DB load
            String prefix = apiKey.substring(0, 10);
            String cacheKey = "apikey:valid:" + prefix;
            String cached = redis.opsForValue().get(cacheKey);

            ApiKeyPrincipal principal;
            if (cached != null) {
                principal = deserialize(cached, ApiKeyPrincipal.class);
            } else {
                principal = apiKeyService.validateAndLoad(apiKey);
                redis.opsForValue().set(cacheKey, serialize(principal), Duration.ofMinutes(5));
            }

            // Store in request attribute for downstream use
            TenantContextHolder.set(principal.getOrganizationId(), principal.getApplicationId());

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (InvalidApiKeyException e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid API key\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
```

### 8.7 Spring Boot Application Configuration

```yaml
# application.yml (common)
spring:
  application:
    name: audithub-ingestion-service

  # PostgreSQL (Control Plane)
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/audithub
    username: ${POSTGRES_USER:audithub}
    password: ${POSTGRES_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000

  # Cassandra (Audit Data)
  cassandra:
    contact-points: ${CASSANDRA_HOSTS:localhost}
    port: 9042
    keyspace-name: audithub
    local-datacenter: ap-south-1
    request:
      consistency: LOCAL_QUORUM
      page-size: 500
    connection:
      connect-timeout: 5s
      init-query-timeout: 5s

  # Redis
  data:
    redis:
      cluster:
        nodes: ${REDIS_CLUSTER_NODES:localhost:6379}
      timeout: 1000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10

  # Kafka
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all              # strongest durability
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 1
        compression.type: lz4
        linger.ms: 5         # small batching for throughput
        batch.size: 65536
    consumer:
      group-id: audithub-storage-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false  # manual acknowledgment
      max-poll-records: 500
      fetch-min-size: 1024

# JWT
jwt:
  secret: ${JWT_SECRET}
  access-token-expiry-seconds: 900      # 15 minutes
  refresh-token-expiry-seconds: 2592000 # 30 days

# AWS S3
aws:
  region: ap-south-1
  s3:
    reports-bucket: audithub-reports-prod
    
# Resilience4j — circuit breaker for external calls
resilience4j:
  circuitbreaker:
    instances:
      geoip-service:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 30s
        failureRateThreshold: 50

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```
