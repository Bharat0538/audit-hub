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
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyClass
public class DailyStatsKey implements Serializable {

    @PrimaryKeyColumn(name = "organization_id", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private UUID organizationId;

    @PrimaryKeyColumn(name = "application_id", type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private UUID applicationId;

    @PrimaryKeyColumn(name = "stat_date", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING, ordinal = 2)
    private LocalDate statDate;

    @PrimaryKeyColumn(name = "action_type", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING, ordinal = 3)
    private String actionType;
}
