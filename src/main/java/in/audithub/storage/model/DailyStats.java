package in.audithub.storage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("daily_stats")
public class DailyStats {

    @PrimaryKey
    private DailyStatsKey key;

    @Column("total_events")
    private long totalEvents;

    @Column("success_count")
    private long successCount;

    @Column("failure_count")
    private long failureCount;

    @Column("critical_count")
    private long criticalCount;

    @Column("unique_users")
    private long uniqueUsers;
}
