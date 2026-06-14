package in.audithub.storage.service;

import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import in.audithub.ingestion.model.EnrichedAuditEvent;
import in.audithub.ingestion.model.Outcome;
import in.audithub.ingestion.model.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyStatsUpdater {

    private final CassandraOperations cassandraOps;
    private final StringRedisTemplate redisTemplate;

    public void increment(EnrichedAuditEvent event) {
        CompletableFuture.runAsync(() -> {
            try {
                LocalDate statDate = LocalDate.ofInstant(event.getEventTime(), ZoneId.of("Asia/Kolkata"));
                String actionType = event.getAction().getType() != null ? event.getAction().getType().name() : "UNKNOWN";

                long successInc = event.getOutcome() == Outcome.SUCCESS ? 1L : 0L;
                long failureInc = event.getOutcome() == Outcome.FAILURE ? 1L : 0L;
                long criticalInc = event.getSeverity() == Severity.CRITICAL ? 1L : 0L;

                // Build atomic counter update statement
                SimpleStatement stmt = QueryBuilder.update("daily_stats")
                        .increment("total_events", literal(1L))
                        .increment("success_count", literal(successInc))
                        .increment("failure_count", literal(failureInc))
                        .increment("critical_count", literal(criticalInc))
                        .increment("unique_users", literal(1L)) // Default to incrementing unique users count
                        .whereColumn("organization_id").isEqualTo(literal(event.getOrganizationId()))
                        .whereColumn("application_id").isEqualTo(literal(event.getApplicationId()))
                        .whereColumn("stat_date").isEqualTo(literal(statDate))
                        .whereColumn("action_type").isEqualTo(literal(actionType))
                        .build();

                cassandraOps.getCqlOperations().execute(stmt);

                // Invalidate Redis dashboard cache
                String orgKey = "dashboard:stats:" + event.getOrganizationId();
                String appKey = "dashboard:stats:" + event.getOrganizationId() + ":" + event.getApplicationId();
                redisTemplate.delete(List.of(orgKey, appKey));

                // Increment top-actors sorted set in Redis
                String topActorsKey = "top-actors:" + event.getOrganizationId() + ":" + java.time.YearMonth.from(LocalDate.ofInstant(event.getEventTime(), ZoneId.of("Asia/Kolkata")));
                String member = event.getActor().getUserId() + ":" + (event.getActor().getUserName() != null ? event.getActor().getUserName() : "");
                redisTemplate.opsForZSet().incrementScore(topActorsKey, member, 1.0);
                redisTemplate.expire(topActorsKey, java.time.Duration.ofDays(35));

            } catch (Exception e) {
                log.error("Failed to update daily stats or invalidate cache", e);
            }
        });
    }
}
