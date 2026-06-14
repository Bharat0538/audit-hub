package in.audithub.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption;
import java.util.List;
import java.util.Map;

@Configuration
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${spring.data.cassandra.keyspace-name:audithub}")
    private String keyspaceName;

    @Value("${spring.data.cassandra.contact-points:127.0.0.1}")
    private String contactPoints;

    @Value("${spring.data.cassandra.port:9042}")
    private int port;

    @Value("${spring.data.cassandra.local-datacenter:datacenter1}")
    private String localDatacenter;

    @Override
    protected String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected String getLocalDataCenter() {
        return localDatacenter;
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        return List.of(CreateKeyspaceSpecification.createKeyspace(keyspaceName)
                .ifNotExists()
                .withSimpleReplication(1));
    }

    @Override
    public String[] getEntityBasePackages() {
        return new String[]{"in.audithub"};
    }

    @org.springframework.context.annotation.Bean("cassandraExecutor")
    public java.util.concurrent.Executor cassandraExecutor() {
        return new java.util.concurrent.ThreadPoolExecutor(
                10, 50,
                60L, java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.ArrayBlockingQueue<>(1000),
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
