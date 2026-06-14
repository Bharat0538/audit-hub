package in.audithub.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.audithub.dashboard.dto.DashboardStats;
import in.audithub.tenant.model.Application;
import in.audithub.tenant.model.Organization;
import in.audithub.tenant.repository.ApplicationRepository;
import in.audithub.tenant.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraOperations;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final CassandraOperations cassandraOps;
    private final ApplicationRepository applicationRepository;
    private final OrganizationRepository organizationRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_TTL = Duration.ofMinutes(2);

    public DashboardStats getDashboardStats(UUID orgId, UUID appId) {
        if (orgId == null) {
            return DashboardStats.builder()
                    .totalEventsToday(0).totalEventsThisMonth(0)
                    .quotaUsedPercent(0).monthlyQuota(10000)
                    .criticalEventsToday(0).failedEventsToday(0)
                    .activeApplications(0)
                    .topActors(java.util.Collections.emptyList())
                    .topResources(java.util.Collections.emptyList())
                    .eventsTrend(java.util.Collections.emptyList())
                    .build();
        }
        String cacheKey = appId != null
                ? "dashboard:stats:" + orgId + ":" + appId
                : "dashboard:stats:" + orgId;

        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, DashboardStats.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read dashboard stats cache", e);
        }

        DashboardStats stats = computeStats(orgId, appId);

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(stats), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to write dashboard stats cache", e);
        }

        return stats;
    }

    private DashboardStats computeStats(UUID orgId, UUID appId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        long maxQuota = org.getMaxEventsPerMonth();
        String yearMonthStr = YearMonth.now(ZoneId.of("Asia/Kolkata")).toString();
        
        // Quota usage from Redis
        String quotaKey = String.format("quota:monthly:%s:%s", orgId, yearMonthStr);
        String quotaVal = redisTemplate.opsForValue().get(quotaKey);
        long totalEventsThisMonth = quotaVal != null ? Long.parseLong(quotaVal) : 0L;
        double quotaUsedPercent = maxQuota > 0 ? ((double) totalEventsThisMonth / maxQuota) * 100.0 : 0.0;

        // Active app count
        int activeApps = (int) applicationRepository.countByOrganizationId(orgId);

        // Fetch applications to query daily stats
        List<UUID> targetAppIds = new ArrayList<>();
        if (appId != null) {
            targetAppIds.add(appId);
        } else {
            List<Application> apps = applicationRepository.findByOrganizationId(orgId);
            for (Application app : apps) {
                targetAppIds.add(app.getId());
            }
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        long totalEventsToday = 0;
        long criticalEventsToday = 0;
        long failedEventsToday = 0;

        for (UUID targetAppId : targetAppIds) {
            SimpleStatement stmt = QueryBuilder.selectFrom("daily_stats")
                    .all()
                    .whereColumn("organization_id").isEqualTo(literal(orgId))
                    .whereColumn("application_id").isEqualTo(literal(targetAppId))
                    .whereColumn("stat_date").isEqualTo(literal(today))
                    .build();

            com.datastax.oss.driver.api.core.cql.ResultSet rs = cassandraOps.getCqlOperations().queryForResultSet(stmt);
            for (Row row : rs) {
                totalEventsToday += row.getLong("total_events");
                criticalEventsToday += row.getLong("critical_count");
                failedEventsToday += row.getLong("failure_count");
            }
        }

        // Top Actors from Redis
        String topActorsKey = "top-actors:" + orgId + ":" + yearMonthStr;
        Set<ZSetOperations.TypedTuple<String>> topActorsTuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(topActorsKey, 0, 4);

        List<DashboardStats.ActorStats> topActors = new ArrayList<>();
        if (topActorsTuples != null) {
            for (ZSetOperations.TypedTuple<String> tuple : topActorsTuples) {
                String value = tuple.getValue();
                Double score = tuple.getScore();
                if (value != null) {
                    String[] parts = value.split(":", 2);
                    String userId = parts[0];
                    String userName = parts.length > 1 ? parts[1] : "";
                    topActors.add(new DashboardStats.ActorStats(userId, userName, score != null ? score.longValue() : 0L));
                }
            }
        }

        return DashboardStats.builder()
                .totalEventsToday(totalEventsToday)
                .totalEventsThisMonth(totalEventsThisMonth)
                .quotaUsedPercent(quotaUsedPercent)
                .monthlyQuota(maxQuota)
                .criticalEventsToday(criticalEventsToday)
                .failedEventsToday(failedEventsToday)
                .activeApplications(activeApps)
                .topActors(topActors)
                .topResources(java.util.Collections.emptyList())
                .eventsTrend(java.util.Collections.emptyList())
                .build();
    }

    public Map<String, Object> getTrends(UUID orgId, UUID appId, String startTimeStr, String endTimeStr) {
        if (orgId == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("dataPoints", java.util.Collections.emptyList());
            return empty;
        }
        java.time.Instant startInstant = java.time.Instant.parse(startTimeStr);
        java.time.Instant endInstant = java.time.Instant.parse(endTimeStr);
        LocalDate startDate = startInstant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
        LocalDate endDate = endInstant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();

        List<UUID> targetAppIds = new ArrayList<>();
        if (appId != null) {
            targetAppIds.add(appId);
        } else {
            List<Application> apps = applicationRepository.findByOrganizationId(orgId);
            for (Application app : apps) {
                targetAppIds.add(app.getId());
            }
        }

        Map<LocalDate, Long> totals = new HashMap<>();
        Map<LocalDate, Long> criticals = new HashMap<>();
        Map<LocalDate, Long> failures = new HashMap<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            totals.put(current, 0L);
            criticals.put(current, 0L);
            failures.put(current, 0L);
            current = current.plusDays(1);
        }

        for (UUID targetAppId : targetAppIds) {
            SimpleStatement stmt = QueryBuilder.selectFrom("daily_stats")
                    .all()
                    .whereColumn("organization_id").isEqualTo(literal(orgId))
                    .whereColumn("application_id").isEqualTo(literal(targetAppId))
                    .whereColumn("stat_date").isGreaterThanOrEqualTo(literal(startDate))
                    .whereColumn("stat_date").isLessThanOrEqualTo(literal(endDate))
                    .build();

            try {
                com.datastax.oss.driver.api.core.cql.ResultSet rs = cassandraOps.getCqlOperations().queryForResultSet(stmt);
                for (Row row : rs) {
                    LocalDate date = row.getLocalDate("stat_date");
                    if (date != null && totals.containsKey(date)) {
                        totals.put(date, totals.get(date) + row.getLong("total_events"));
                        criticals.put(date, criticals.get(date) + row.getLong("critical_count"));
                        failures.put(date, failures.get(date) + row.getLong("failure_count"));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to query daily stats for application: {}", targetAppId, e);
            }
        }

        List<Map<String, Object>> dataPoints = new ArrayList<>();
        current = startDate;
        while (!current.isAfter(endDate)) {
            Map<String, Object> pt = new HashMap<>();
            pt.put("date", current.toString());
            pt.put("count", totals.get(current));
            pt.put("criticalCount", criticals.get(current));
            pt.put("failureCount", failures.get(current));
            dataPoints.add(pt);
            current = current.plusDays(1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dataPoints", dataPoints);
        return result;
    }
}
