package in.audithub.query.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.audithub.query.dto.AuditEventPage;
import in.audithub.query.dto.AuditEventResponse;
import in.audithub.query.dto.AuditEventSearchRequest;
import in.audithub.storage.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditQueryService {

    private final AuditEventRepository repository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public AuditEventPage<AuditEventResponse> searchEvents(AuditEventSearchRequest req) {
        // Cache only when filters are simple and result set is small (e.g. first page)
        if (req.getPageToken() == null && isCacheable(req)) {
            String cacheKey = buildCacheKey(req);
            try {
                String cached = redis.opsForValue().get(cacheKey);
                if (cached != null) {
                    AuditEventPage<AuditEventResponse> cachedPage = deserializePage(cached);
                    if (cachedPage != null) {
                        return cachedPage;
                    }
                }
            } catch (Exception e) {
                log.warn("Redis read failed for cacheKey: {}", cacheKey, e);
            }

            AuditEventPage<AuditEventResponse> result = repository.search(req);

            try {
                String serialized = serializePage(result);
                if (serialized != null) {
                    redis.opsForValue().set(cacheKey, serialized, CACHE_TTL);
                }
            } catch (Exception e) {
                log.warn("Redis write failed for cacheKey: {}", cacheKey, e);
            }

            return result;
        }
        return repository.search(req);
    }

    public AuditEventPage<AuditEventResponse> getEntityHistory(UUID orgId, String resourceType,
                                                               String resourceId, Instant start,
                                                               Instant end, String pageToken, int pageSize) {
        return repository.searchByResource(orgId, resourceType, resourceId,
                start, end, pageToken, pageSize);
    }

    public AuditEventPage<AuditEventResponse> getUserActivity(UUID orgId, String userId,
                                                             Instant start, Instant end,
                                                             String pageToken, int pageSize) {
        return repository.searchByUser(orgId, userId, start, end, pageToken, pageSize);
    }

    private boolean isCacheable(AuditEventSearchRequest req) {
        // Only cache simple time-range queries without fine-grained filters
        return req.getActorUserId() == null
                && req.getResourceId() == null
                && (req.getActionTypes() == null || req.getActionTypes().isEmpty())
                && (req.getSeverities() == null || req.getSeverities().isEmpty());
    }

    private String buildCacheKey(AuditEventSearchRequest req) {
        return String.format("query:%s:%s:%s:%s:%d",
                req.getOrganizationId(), req.getApplicationId(),
                req.getStartTime().truncatedTo(ChronoUnit.HOURS),
                req.getEndTime().truncatedTo(ChronoUnit.HOURS),
                req.getPageSize());
    }

    private String serializePage(AuditEventPage<AuditEventResponse> page) {
        try {
            return objectMapper.writeValueAsString(page);
        } catch (Exception e) {
            return null;
        }
    }

    private AuditEventPage<AuditEventResponse> deserializePage(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<AuditEventPage<AuditEventResponse>>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
