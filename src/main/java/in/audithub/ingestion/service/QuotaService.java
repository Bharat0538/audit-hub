package in.audithub.ingestion.service;

import in.audithub.common.exception.QuotaExceededException;
import in.audithub.tenant.model.Organization;
import in.audithub.tenant.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuotaService {

    private final StringRedisTemplate redis;
    private final OrganizationRepository organizationRepository;

    private static final String QUOTA_KEY = "quota:monthly:%s:%s"; // orgId:yearMonth

    public void checkAndIncrement(UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        String key = quotaKey(orgId);
        Long current = redis.opsForValue().increment(key);
        if (current == null) {
            current = 1L;
        }
        if (current == 1) {
            redis.expire(key, Duration.ofDays(35));
        }
        if (current > org.getMaxEventsPerMonth()) {
            redis.opsForValue().decrement(key); // rollback
            throw new QuotaExceededException(
                    "Monthly event quota of " + org.getMaxEventsPerMonth() + " exceeded");
        }
    }

    public void checkAndIncrementBatch(UUID orgId, int count) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        String key = quotaKey(orgId);
        Long current = redis.opsForValue().increment(key, count);
        if (current == null) {
            current = (long) count;
        }
        if (current == count) {
            redis.expire(key, Duration.ofDays(35));
        }
        if (current > org.getMaxEventsPerMonth()) {
            redis.opsForValue().decrement(key, count); // rollback
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
