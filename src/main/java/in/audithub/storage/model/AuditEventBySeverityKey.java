package in.audithub.storage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyClass
public class AuditEventBySeverityKey implements Serializable {

    @PrimaryKeyColumn(name = "organization_id", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private UUID organizationId;

    @PrimaryKeyColumn(name = "severity", type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private String severity;

    @PrimaryKeyColumn(name = "month_bucket", type = PrimaryKeyType.PARTITIONED, ordinal = 2)
    private String monthBucket;

    @PrimaryKeyColumn(name = "event_time", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING, ordinal = 3)
    private Instant eventTime;

    @PrimaryKeyColumn(name = "event_id", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING, ordinal = 4)
    private UUID eventId;
}
