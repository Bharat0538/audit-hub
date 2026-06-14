package in.audithub.ingestion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyChecker {

    private final StringRedisTemplate redis;
    private static final String IDEM_KEY_PREFIX = "idem:%s:%s"; // orgId:eventId

    public boolean isDuplicate(UUID orgId, UUID eventId) {
        String key = String.format(IDEM_KEY_PREFIX, orgId, eventId);
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    public void mark(UUID orgId, UUID eventId) {
        String key = String.format(IDEM_KEY_PREFIX, orgId, eventId);
        redis.opsForValue().set(key, "1", Duration.ofHours(24));
    }
}
