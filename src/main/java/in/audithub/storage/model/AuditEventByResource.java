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
@Table("audit_events_by_resource")
public class AuditEventByResource {

    @PrimaryKey
    private AuditEventByResourceKey key;

    @Column("application_id")
    private UUID applicationId;

    @Column("actor_user_id")
    private String actorUserId;

    @Column("actor_user_email")
    private String actorUserEmail;

    @Column("action_type")
    private String actionType;

    @Column("action_name")
    private String actionName;

    @Column("changes")
    private String changes;

    @Column("outcome")
    private String outcome;

    @Column("severity")
    private String severity;
}
