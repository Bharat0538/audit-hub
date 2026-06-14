package in.audithub.sdk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

@Data
@ConfigurationProperties(prefix = "audithub")
public class AuditHubProperties {
    private boolean enabled = true;
    private String apiKey;
    private UUID applicationId;
    private String baseUrl = "http://localhost:8080";
    private boolean async = true;
    private int batchSize = 100;
    private long flushIntervalMs = 500;
    private int timeoutSeconds = 5;
    private int retryMaxAttempts = 3;
}
