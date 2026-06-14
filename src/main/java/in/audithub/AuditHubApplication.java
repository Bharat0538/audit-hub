package in.audithub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraReactiveRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        CassandraRepositoriesAutoConfiguration.class,
        CassandraReactiveRepositoriesAutoConfiguration.class
})
@EnableAsync
@EnableScheduling
@EntityScan(basePackages = {
        "in.audithub.alert.model",
        "in.audithub.iam.model",
        "in.audithub.report.model",
        "in.audithub.tenant.model"
})
@EnableJpaRepositories(basePackages = {
        "in.audithub.alert.repository",
        "in.audithub.iam.repository",
        "in.audithub.report.repository",
        "in.audithub.tenant.repository"
})
public class AuditHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditHubApplication.class, args);
    }
}
