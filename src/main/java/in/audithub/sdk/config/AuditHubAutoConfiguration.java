package in.audithub.sdk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.audithub.sdk.aspect.AuditAspect;
import in.audithub.sdk.client.AuditHubClient;
import in.audithub.sdk.context.AuditContextProvider;
import in.audithub.sdk.context.SpringSecurityAuditContextProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(AuditHubProperties.class)
@ConditionalOnProperty(prefix = "audithub", name = "enabled", matchIfMissing = true)
public class AuditHubAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditHubClient auditHubClient(AuditHubProperties props) {
        return new AuditHubClient(props);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditAspect auditAspect(AuditHubClient client, AuditContextProvider provider, ObjectMapper objectMapper) {
        return new AuditAspect(client, provider, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditContextProvider auditContextProvider() {
        return new SpringSecurityAuditContextProvider();
    }
}
