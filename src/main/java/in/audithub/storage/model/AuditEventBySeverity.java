package in.audithub.storage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("audit_events_by_severity")
public class AuditEventBySeverity {

    @PrimaryKey
    private AuditEventBySeverityKey key;

    @Column("application_id")
    private UUID applicationId;

    @Column("actor_user_id")
    private String actorUserId;

    @Column("action_type")
    private String actionType;

    @Column("action_name")
    private String actionName;

    @Column("resource_type")
    private String resourceType;

    @Column("resource_id")
    private String resourceId;

    @Column("outcome")
    private String outcome;
}
