package in.audithub.storage.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.relation.Relation;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import in.audithub.ingestion.model.*;
import in.audithub.query.dto.AuditEventPage;
import in.audithub.query.dto.AuditEventResponse;
import in.audithub.query.dto.AuditEventSearchRequest;
import in.audithub.storage.service.TenantRetentionService;
import in.audithub.tenant.repository.ApplicationRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Repository
@RequiredArgsConstructor
public class AuditEventRepository {

    private final CassandraOperations cassandraOps;
    private final TenantRetentionService retentionService;
    private final ObjectMapper objectMapper;
    private final java.util.concurrent.Executor cassandraExecutor;
    private final ApplicationRepository applicationRepository;

    private String serializeChanges(List<FieldChange> changes) {
        if (changes == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (Exception e) {
            return "[]";
        }
    }

    public CompletableFuture<Void> saveToMainTable(EnrichedAuditEvent event) {
        int ttlSeconds = retentionService.getRetentionTtlSeconds(event.getOrganizationId());

        SimpleStatement stmt = QueryBuilder.insertInto("audit_events")
                .value("organization_id", literal(event.getOrganizationId()))
                .value("application_id", literal(event.getApplicationId()))
                .value("month_bucket", literal(event.getMonthBucket()))
                .value("event_time", literal(event.getEventTime()))
                .value("event_id", literal(event.getEventId()))
                .value("actor_user_id", literal(event.getActor().getUserId()))
                .value("actor_user_email", literal(event.getActor().getUserEmail()))
                .value("actor_user_name", literal(event.getActor().getUserName()))
                .value("actor_ip_address", literal(event.getActor().getIpAddress()))
                .value("actor_user_agent", literal(event.getActor().getUserAgent()))
                .value("actor_session_id", literal(event.getActor().getSessionId()))
                .value("action_type", literal(event.getAction().getType().name()))
                .value("action_name", literal(event.getAction().getName()))
                .value("action_description", literal(event.getAction().getDescription()))
                .value("resource_type", literal(event.getResource().getType()))
                .value("resource_id", literal(event.getResource().getId()))
                .value("resource_name", literal(event.getResource().getName()))
                .value("resource_path", literal(event.getResource().getPath()))
                .value("changes", literal(serializeChanges(event.getChanges())))
                .value("outcome", literal(event.getOutcome().name()))
                .value("severity", literal(event.getSeverity().name()))
                .value("metadata", literal(event.getMetadata()))
                .value("correlation_id", literal(event.getCorrelationId()))
                .value("tags", literal(event.getTags()))
                .value("raw_payload", literal(event.getRawPayload()))
                .usingTtl(ttlSeconds)
                .build();

        return CompletableFuture.runAsync(() ->
                cassandraOps.getCqlOperations().execute(stmt));
    }

    public CompletableFuture<Void> saveToUserTable(EnrichedAuditEvent event) {
        int ttlSeconds = retentionService.getRetentionTtlSeconds(event.getOrganizationId());

        SimpleStatement stmt = QueryBuilder.insertInto("audit_events_by_user")
                .value("organization_id", literal(event.getOrganizationId()))
                .value("actor_user_id", literal(event.getActor().getUserId()))
                .value("month_bucket", literal(event.getMonthBucket()))
                .value("event_time", literal(event.getEventTime()))
                .value("event_id", literal(event.getEventId()))
                .value("application_id", literal(event.getApplicationId()))
                .value("action_type", literal(event.getAction().getType().name()))
                .value("action_name", literal(event.getAction().getName()))
                .value("resource_type", literal(event.getResource().getType()))
                .value("resource_id", literal(event.getResource().getId()))
                .value("resource_name", literal(event.getResource().getName()))
                .value("outcome", literal(event.getOutcome().name()))
                .value("severity", literal(event.getSeverity().name()))
                .value("actor_ip_address", literal(event.getActor().getIpAddress()))
                .usingTtl(ttlSeconds)
                .build();

        return CompletableFuture.runAsync(() ->
                cassandraOps.getCqlOperations().execute(stmt));
    }

    public CompletableFuture<Void> saveToResourceTable(EnrichedAuditEvent event) {
        int ttlSeconds = retentionService.getRetentionTtlSeconds(event.getOrganizationId());

        SimpleStatement stmt = QueryBuilder.insertInto("audit_events_by_resource")
                .value("organization_id", literal(event.getOrganizationId()))
                .value("resource_type", literal(event.getResource().getType()))
                .value("resource_id", literal(event.getResource().getId()))
                .value("event_time", literal(event.getEventTime()))
                .value("event_id", literal(event.getEventId()))
                .value("application_id", literal(event.getApplicationId()))
                .value("actor_user_id", literal(event.getActor().getUserId()))
                .value("actor_user_email", literal(event.getActor().getUserEmail()))
                .value("action_type", literal(event.getAction().getType().name()))
                .value("action_name", literal(event.getAction().getName()))
                .value("changes", literal(serializeChanges(event.getChanges())))
                .value("outcome", literal(event.getOutcome().name()))
                .value("severity", literal(event.getSeverity().name()))
                .usingTtl(ttlSeconds)
                .build();

        return CompletableFuture.runAsync(() ->
                cassandraOps.getCqlOperations().execute(stmt));
    }

    public CompletableFuture<Void> saveToSeverityTable(EnrichedAuditEvent event) {
        int ttlSeconds = retentionService.getRetentionTtlSeconds(event.getOrganizationId());

        SimpleStatement stmt = QueryBuilder.insertInto("audit_events_by_severity")
                .value("organization_id", literal(event.getOrganizationId()))
                .value("severity", literal(event.getSeverity().name()))
                .value("month_bucket", literal(event.getMonthBucket()))
                .value("event_time", literal(event.getEventTime()))
                .value("event_id", literal(event.getEventId()))
                .value("application_id", literal(event.getApplicationId()))
                .value("actor_user_id", literal(event.getActor().getUserId()))
                .value("action_type", literal(event.getAction().getType().name()))
                .value("action_name", literal(event.getAction().getName()))
                .value("resource_type", literal(event.getResource().getType()))
                .value("resource_id", literal(event.getResource().getId()))
                .value("outcome", literal(event.getOutcome().name()))
                .usingTtl(ttlSeconds)
                .build();

        return CompletableFuture.runAsync(() ->
                cassandraOps.getCqlOperations().execute(stmt));
    }

    public CompletableFuture<Void> saveToEntityTimeline(EnrichedAuditEvent event) {
        int ttlSeconds = retentionService.getRetentionTtlSeconds(event.getOrganizationId());

        SimpleStatement stmt = QueryBuilder.insertInto("entity_timeline")
                .value("organization_id", literal(event.getOrganizationId()))
                .value("resource_type", literal(event.getResource().getType()))
                .value("resource_id", literal(event.getResource().getId()))
                .value("event_time", literal(event.getEventTime()))
                .value("event_id", literal(event.getEventId()))
                .value("actor_user_id", literal(event.getActor().getUserId()))
                .value("actor_user_name", literal(event.getActor().getUserName()))
                .value("action_type", literal(event.getAction().getType().name()))
                .value("action_name", literal(event.getAction().getName()))
                .value("action_description", literal(event.getAction().getDescription()))
                .value("changes", literal(serializeChanges(event.getChanges())))
                .value("outcome", literal(event.getOutcome().name()))
                .value("severity", literal(event.getSeverity().name()))
                .usingTtl(ttlSeconds)
                .build();

        return CompletableFuture.runAsync(() ->
                cassandraOps.getCqlOperations().execute(stmt));
    }

    public AuditEventPage<AuditEventResponse> search(AuditEventSearchRequest req) {
        int pageSize = req.getPageSize() <= 0 ? 50 : req.getPageSize();
        List<String> monthBuckets = generateMonthBuckets(req.getStartTime(), req.getEndTime());

        // Resolve application IDs to query
        List<UUID> appIds;
        if (req.getApplicationId() != null) {
            appIds = List.of(req.getApplicationId());
        } else {
            // All Applications mode — fan out across all apps in the org
            appIds = applicationRepository.findByOrganizationId(req.getOrganizationId())
                    .stream()
                    .map(app -> app.getId())
                    .collect(Collectors.toList());
        }

        if (appIds.isEmpty()) {
            return AuditEventPage.of(Collections.emptyList(), null, false);
        }

        List<CompletableFuture<List<AuditEventResponse>>> futures = new ArrayList<>();
        for (String bucket : monthBuckets) {
            for (UUID appId : appIds) {
                final UUID finalAppId = appId;
                AuditEventSearchRequest reqForApp = AuditEventSearchRequest.builder()
                        .organizationId(req.getOrganizationId())
                        .applicationId(finalAppId)
                        .startTime(req.getStartTime())
                        .endTime(req.getEndTime())
                        .actionTypes(req.getActionTypes())
                        .severities(req.getSeverities())
                        .actorUserId(req.getActorUserId())
                        .resourceId(req.getResourceId())
                        .pageSize(pageSize)
                        .build();
                futures.add(CompletableFuture.supplyAsync(
                        () -> queryBucket(bucket, reqForApp, pageSize), cassandraExecutor));
            }
        }

        List<AuditEventResponse> combinedResults = new ArrayList<>();
        for (CompletableFuture<List<AuditEventResponse>> future : futures) {
            try {
                combinedResults.addAll(future.join());
            } catch (Exception e) {
                // Log/Ignore
            }
        }

        combinedResults.sort((e1, e2) -> e2.getEventTime().compareTo(e1.getEventTime()));

        List<AuditEventResponse> pagedResults = combinedResults.stream()
                .limit(pageSize)
                .toList();

        return AuditEventPage.of(pagedResults, null, false);
    }

    private List<AuditEventResponse> queryBucket(String bucket, AuditEventSearchRequest req, int limit) {
        if (req.getApplicationId() == null) {
            return Collections.emptyList(); // safety guard
        }
        List<AuditEventResponse> bucketResults = new ArrayList<>();
        try {
            Select select = QueryBuilder.selectFrom("audit_events")
                    .all()
                    .where(Relation.column("organization_id").isEqualTo(literal(req.getOrganizationId())))
                    .where(Relation.column("application_id").isEqualTo(literal(req.getApplicationId())))
                    .where(Relation.column("month_bucket").isEqualTo(literal(bucket)))
                    .where(Relation.column("event_time").isGreaterThanOrEqualTo(literal(req.getStartTime())))
                    .where(Relation.column("event_time").isLessThanOrEqualTo(literal(req.getEndTime())));

            SimpleStatement stmt = select.build().setPageSize(limit);
            ResultSet rs = cassandraOps.getCqlOperations().queryForResultSet(stmt);

            int count = 0;
            while (count < limit) {
                Row row = rs.one();
                if (row != null) {
                    AuditEventResponse response = mapRow(row);
                    if (matchesFilters(response, req)) {
                        bucketResults.add(response);
                        count++;
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            // Log/Ignore
        }
        return bucketResults;
    }

    private boolean matchesFilters(AuditEventResponse event, AuditEventSearchRequest req) {
        if (event == null) {
            return false;
        }
        if (req.getActionTypes() != null && !req.getActionTypes().isEmpty()) {
            if (event.getAction() == null || event.getAction().getType() == null ||
                    !req.getActionTypes().contains(event.getAction().getType().name())) {
                return false;
            }
        }
        if (req.getSeverities() != null && !req.getSeverities().isEmpty()) {
            if (event.getSeverity() == null ||
                    !req.getSeverities().contains(event.getSeverity().name())) {
                return false;
            }
        }
        if (req.getActorUserId() != null && !req.getActorUserId().isEmpty()) {
            if (event.getActor() == null ||
                    !req.getActorUserId().equals(event.getActor().getUserId())) {
                return false;
            }
        }
        if (req.getResourceId() != null && !req.getResourceId().isEmpty()) {
            if (event.getResource() == null ||
                    !req.getResourceId().equals(event.getResource().getId())) {
                return false;
            }
        }
        return true;
    }

    public AuditEventPage<AuditEventResponse> searchByResource(UUID orgId, String resourceType, String resourceId,
                                                               Instant start, Instant end, String pageToken, int pageSize) {
        PagingTokenState tokenState = decodePagingToken(pageToken);
        String pagingStateStr = tokenState.getPagingState();

        Select select = QueryBuilder.selectFrom("audit_events_by_resource")
                .all()
                .where(Relation.column("organization_id").isEqualTo(literal(orgId)))
                .where(Relation.column("resource_type").isEqualTo(literal(resourceType)))
                .where(Relation.column("resource_id").isEqualTo(literal(resourceId)))
                .where(Relation.column("event_time").isGreaterThanOrEqualTo(literal(start)))
                .where(Relation.column("event_time").isLessThanOrEqualTo(literal(end)));

        SimpleStatement stmt = select.build().setPageSize(pageSize);

        if (pagingStateStr != null) {
            ByteBuffer pagingStateBuf = ByteBuffer.wrap(Base64.getDecoder().decode(pagingStateStr));
            stmt = stmt.setPagingState(pagingStateBuf);
        }

        ResultSet rs = cassandraOps.getCqlOperations().queryForResultSet(stmt);
        List<AuditEventResponse> allResults = new ArrayList<>();
        int availableWithoutFetching = rs.getAvailableWithoutFetching();

        for (int j = 0; j < availableWithoutFetching; j++) {
            Row row = rs.one();
            if (row != null) {
                allResults.add(mapRow(row));
            }
        }

        if (!rs.isFullyFetched() || rs.getExecutionInfo().getPagingState() != null) {
            ByteBuffer nextState = rs.getExecutionInfo().getPagingState();
            String nextPageToken = encodePagingToken(nextState, 0);
            return AuditEventPage.of(allResults, nextPageToken, true);
        }

        return AuditEventPage.of(allResults, null, false);
    }

    public AuditEventPage<AuditEventResponse> searchByUser(UUID orgId, String userId, Instant start, Instant end,
                                                           String pageToken, int pageSize) {
        List<String> monthBuckets = generateMonthBuckets(start, end);
        List<AuditEventResponse> allResults = new ArrayList<>();
        PagingTokenState tokenState = decodePagingToken(pageToken);

        int startBucketIndex = tokenState.getBucketIndex();
        String pagingStateStr = tokenState.getPagingState();

        for (int i = startBucketIndex; i < monthBuckets.size(); i++) {
            String bucket = monthBuckets.get(i);

            Select select = QueryBuilder.selectFrom("audit_events_by_user")
                    .all()
                    .where(Relation.column("organization_id").isEqualTo(literal(orgId)))
                    .where(Relation.column("actor_user_id").isEqualTo(literal(userId)))
                    .where(Relation.column("month_bucket").isEqualTo(literal(bucket)))
                    .where(Relation.column("event_time").isGreaterThanOrEqualTo(literal(start)))
                    .where(Relation.column("event_time").isLessThanOrEqualTo(literal(end)));

            SimpleStatement stmt = select.build().setPageSize(pageSize);

            if (i == startBucketIndex && pagingStateStr != null) {
                ByteBuffer pagingStateBuf = ByteBuffer.wrap(Base64.getDecoder().decode(pagingStateStr));
                stmt = stmt.setPagingState(pagingStateBuf);
            }

            ResultSet rs = cassandraOps.getCqlOperations().queryForResultSet(stmt);
            int availableWithoutFetching = rs.getAvailableWithoutFetching();

            for (int j = 0; j < availableWithoutFetching; j++) {
                Row row = rs.one();
                if (row != null) {
                    allResults.add(mapRow(row));
                }
            }

            if (!rs.isFullyFetched() || rs.getExecutionInfo().getPagingState() != null) {
                ByteBuffer nextState = rs.getExecutionInfo().getPagingState();
                String nextPageToken = encodePagingToken(nextState, i);
                return AuditEventPage.of(allResults, nextPageToken, true);
            }
        }

        return AuditEventPage.of(allResults, null, false);
    }

    private PagingTokenState decodePagingToken(String pageToken) {
        if (pageToken == null || pageToken.isEmpty()) {
            return new PagingTokenState(null, 0);
        }
        try {
            String decodedJson = new String(Base64.getDecoder().decode(pageToken));
            return objectMapper.readValue(decodedJson, PagingTokenState.class);
        } catch (Exception e) {
            return new PagingTokenState(null, 0);
        }
    }

    private String encodePagingToken(ByteBuffer pagingState, int bucketIndex) {
        if (pagingState == null) {
            return null;
        }
        try {
            byte[] bytes = new byte[pagingState.remaining()];
            pagingState.get(bytes);
            String pagingStateStr = Base64.getEncoder().encodeToString(bytes);
            PagingTokenState state = new PagingTokenState(pagingStateStr, bucketIndex);
            String json = objectMapper.writeValueAsString(state);
            return Base64.getEncoder().encodeToString(json.getBytes());
        } catch (Exception e) {
            return null;
        }
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

    private String getStringSafe(Row row, String column) {
        return row.getColumnDefinitions().contains(column) ? row.getString(column) : null;
    }

    private UUID getUuidSafe(Row row, String column) {
        return row.getColumnDefinitions().contains(column) ? row.getUuid(column) : null;
    }

    private Instant getInstantSafe(Row row, String column) {
        return row.getColumnDefinitions().contains(column) ? row.getInstant(column) : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getMapSafe(Row row, String column) {
        return row.getColumnDefinitions().contains(column) ? (Map<String, String>) row.getMap(column, String.class, String.class) : null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getListSafe(Row row, String column) {
        return row.getColumnDefinitions().contains(column) ? (List<String>) row.getList(column, String.class) : null;
    }

    private AuditEventResponse mapRow(Row row) {
        if (row == null) return null;

        String changesStr = getStringSafe(row, "changes");
        List<FieldChange> changes = null;
        if (changesStr != null) {
            try {
                changes = objectMapper.readValue(changesStr, new TypeReference<List<FieldChange>>() {});
            } catch (Exception e) {
                changes = new ArrayList<>();
            }
        } else {
            changes = new ArrayList<>();
        }

        Actor actor = Actor.builder()
                .userId(getStringSafe(row, "actor_user_id"))
                .userEmail(getStringSafe(row, "actor_user_email"))
                .userName(getStringSafe(row, "actor_user_name"))
                .ipAddress(getStringSafe(row, "actor_ip_address"))
                .userAgent(getStringSafe(row, "actor_user_agent"))
                .sessionId(getStringSafe(row, "actor_session_id"))
                .build();

        String actionTypeStr = getStringSafe(row, "action_type");
        ActionType actionType = null;
        if (actionTypeStr != null) {
            try {
                actionType = ActionType.valueOf(actionTypeStr);
            } catch (Exception e) {
                // Keep null
            }
        }
        Action action = Action.builder()
                .type(actionType)
                .name(getStringSafe(row, "action_name"))
                .description(getStringSafe(row, "action_description"))
                .build();

        Resource resource = Resource.builder()
                .type(getStringSafe(row, "resource_type"))
                .id(getStringSafe(row, "resource_id"))
                .name(getStringSafe(row, "resource_name"))
                .path(getStringSafe(row, "resource_path"))
                .build();

        String outcomeStr = getStringSafe(row, "outcome");
        Outcome outcome = null;
        if (outcomeStr != null) {
            try {
                outcome = Outcome.valueOf(outcomeStr);
            } catch (Exception e) {
                // Keep null
            }
        }

        String severityStr = getStringSafe(row, "severity");
        Severity severity = null;
        if (severityStr != null) {
            try {
                severity = Severity.valueOf(severityStr);
            } catch (Exception e) {
                // Keep null
            }
        }

        return AuditEventResponse.builder()
                .eventId(getUuidSafe(row, "event_id"))
                .organizationId(getUuidSafe(row, "organization_id"))
                .applicationId(getUuidSafe(row, "application_id"))
                .eventTime(getInstantSafe(row, "event_time"))
                .monthBucket(getStringSafe(row, "month_bucket"))
                .actor(actor)
                .action(action)
                .resource(resource)
                .changes(changes)
                .outcome(outcome)
                .severity(severity)
                .correlationId(getStringSafe(row, "correlation_id"))
                .metadata(getMapSafe(row, "metadata"))
                .tags(getListSafe(row, "tags"))
                .rawPayload(getStringSafe(row, "raw_payload"))
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagingTokenState implements Serializable {
        private String pagingState;
        private int bucketIndex;
    }
}
