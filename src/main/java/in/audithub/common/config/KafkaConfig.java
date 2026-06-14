package in.audithub.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
public class KafkaConfig {

    public static final String AUDIT_RAW       = "audit.events.raw";
    public static final String AUDIT_ENRICHED  = "audit.events.enriched";
    public static final String AUDIT_DLQ       = "audit.events.dlq";
    public static final String AUDIT_REPLAY    = "audit.events.replay";
    public static final String AUDIT_ALERTS    = "audit.alerts";

    @Bean
    public NewTopic rawEventsTopic() {
        return TopicBuilder.name(AUDIT_RAW)
                .partitions(8)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic enrichedEventsTopic() {
        return TopicBuilder.name(AUDIT_ENRICHED)
                .partitions(8)
                .replicas(1)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> auditKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
