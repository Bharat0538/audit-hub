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
@Table("audit_events_by_user")
public class AuditEventByUser {

    @PrimaryKey
    private AuditEventByUserKey key;

    @Column("application_id")
    private UUID applicationId;

    @Column("action_type")
    private String actionType;

    @Column("action_name")
    private String actionName;

    @Column("resource_type")
    private String resourceType;

    @Column("resource_id")
    private String resourceId;

    @Column("resource_name")
    private String resourceName;

    @Column("outcome")
    private String outcome;

    @Column("severity")
    private String severity;

    @Column("actor_ip_address")
    private String actorIpAddress;
}
