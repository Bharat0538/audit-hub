package in.audithub.alert.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.audithub.alert.model.AlertRule;
import in.audithub.alert.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertRuleService {

    private final AlertRuleRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public List<AlertRule> getActiveRules(UUID orgId) {
        String cacheKey = "alert:rules:" + orgId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<AlertRule>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to read alert rules cache", e);
        }

        List<AlertRule> rules = repository.findByOrganizationIdAndIsActiveTrue(orgId);

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(rules), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to write alert rules cache", e);
        }

        return rules;
    }

    public AlertRule createRule(AlertRule rule) {
        AlertRule saved = repository.save(rule);
        invalidateCache(rule.getOrganizationId());
        return saved;
    }

    public AlertRule updateRule(AlertRule rule) {
        AlertRule saved = repository.save(rule);
        invalidateCache(rule.getOrganizationId());
        return saved;
    }

    public void deleteRule(UUID ruleId) {
        repository.findById(ruleId).ifPresent(rule -> {
            repository.deleteById(ruleId);
            invalidateCache(rule.getOrganizationId());
        });
    }

    public List<AlertRule> getRulesByOrg(UUID orgId) {
        return repository.findByOrganizationId(orgId);
    }

    private void invalidateCache(UUID orgId) {
        String cacheKey = "alert:rules:" + orgId;
        redisTemplate.delete(cacheKey);
    }
}
