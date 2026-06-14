package in.audithub.storage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.audithub.alert.model.AlertRule;
import in.audithub.alert.model.ConditionConfig;
import in.audithub.alert.service.AlertRuleService;
import in.audithub.ingestion.model.EnrichedAuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertEvaluator {

    private final AlertRuleService ruleService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final in.audithub.tenant.repository.ApplicationRepository applicationRepository;

    public void evaluate(EnrichedAuditEvent event) {
        try {
            List<AlertRule> rules = ruleService.getActiveRules(event.getOrganizationId());
            for (AlertRule rule : rules) {
                if (rule.getApplicationId() != null && !rule.getApplicationId().equals(event.getApplicationId())) {
                    continue;
                }

                ConditionConfig config = objectMapper.readValue(rule.getConditionConfig(), ConditionConfig.class);

                // Match basic conditions
                if (config.getActionType() != null && !config.getActionType().equals(event.getAction().getType().name())) {
                    continue;
                }
                if (config.getResourceType() != null && !config.getResourceType().equals(event.getResource().getType())) {
                    continue;
                }

                // Rolling window key
                // Group by actor_user_id or simple global rule
                String groupValue = "global";
                if ("actor_user_id".equals(config.getGroupBy())) {
                    groupValue = event.getActor().getUserId();
                }
                String countKey = "alert:" + rule.getId() + ":" + groupValue;

                long nowMs = Instant.now().toEpochMilli();
                long windowStartMs = nowMs - (config.getWindowMinutes() * 60L * 1000L);

                // Add current event to ZSet
                redisTemplate.opsForZSet().add(countKey, event.getEventId().toString(), nowMs);

                // Clean older events out of rolling window
                redisTemplate.opsForZSet().removeRangeByScore(countKey, 0, windowStartMs - 1);

                // Count size of window
                Long count = redisTemplate.opsForZSet().zCard(countKey);
                redisTemplate.expire(countKey, Duration.ofMinutes(config.getWindowMinutes() + 1));

                if (count != null && count >= config.getThreshold()) {
                    String cooldownKey = "alert:cooldown:" + rule.getId() + ":" + groupValue;
                    Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);
                    if (hasCooldown == null || !hasCooldown) {
                        // Set 15-minute cooldown to prevent alert storms
                        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofMinutes(15));
                        log.warn("ALERT TRIGGERED! Rule: {} ({}) - Threshold {} met for group: {} (Count: {})",
                                rule.getName(), rule.getSeverity(), config.getThreshold(), groupValue, count);
                        
                        triggerWebhook(event, rule, count, groupValue);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to evaluate alert rules for event: {}", event.getEventId(), e);
        }
    }

    private void triggerWebhook(EnrichedAuditEvent event, AlertRule rule, long count, String groupValue) {
        try {
            in.audithub.tenant.model.Application app = applicationRepository.findById(event.getApplicationId()).orElse(null);
            if (app != null && app.getWebhookUrl() != null && !app.getWebhookUrl().isEmpty()) {
                in.audithub.alert.dto.WebhookPayload payload = in.audithub.alert.dto.WebhookPayload.builder()
                        .alertRuleId(rule.getId())
                        .alertRuleName(rule.getName())
                        .severity(rule.getSeverity())
                        .triggerCount(count)
                        .groupByValue(groupValue)
                        .timestamp(Instant.now())
                        .triggeringEvent(event)
                        .build();

                String secret = app.getWebhookSecret() != null ? app.getWebhookSecret() : "default-secret";
                sendWebhookAsync(app.getWebhookUrl(), secret, payload);
            }
        } catch (Exception e) {
            log.error("Failed to trigger webhook for rule {}: {}", rule.getId(), e.getMessage());
        }
    }

    private void sendWebhookAsync(String url, String secret, in.audithub.alert.dto.WebhookPayload payload) {
        CompletableFuture.runAsync(() -> {
            try {
                String body = objectMapper.writeValueAsString(payload);
                String signature = computeHmacSha256(body, secret);

                org.springframework.web.client.RestClient.create()
                        .post()
                        .uri(url)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header("X-AuditHub-Signature-256", "sha256=" + signature)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Alert webhook successfully sent to {}", url);
            } catch (Exception e) {
                log.error("Failed to deliver alert webhook to {}: {}", url, e.getMessage());
            }
        });
    }

    private String computeHmacSha256(String data, String key) {
        try {
            javax.crypto.Mac sha256HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] hash = sha256HMAC.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }
}
