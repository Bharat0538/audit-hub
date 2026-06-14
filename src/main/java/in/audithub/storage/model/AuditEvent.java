package in.audithub.storage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("audit_events")
public class AuditEvent {

    @PrimaryKey
    private AuditEventKey key;

    @Column("actor_user_id")
    private String actorUserId;

    @Column("actor_user_email")
    private String actorUserEmail;

    @Column("actor_user_name")
    private String actorUserName;

    @Column("actor_ip_address")
    private String actorIpAddress;

    @Column("actor_user_agent")
    private String actorUserAgent;

    @Column("actor_session_id")
    private String actorSessionId;

    @Column("action_type")
    private String actionType;

    @Column("action_name")
    private String actionName;

    @Column("action_description")
    private String actionDescription;

    @Column("resource_type")
    private String resourceType;

    @Column("resource_id")
    private String resourceId;

    @Column("resource_name")
    private String resourceName;

    @Column("resource_path")
    private String resourcePath;

    @Column("changes")
    private String changes;

    @Column("outcome")
    private String outcome;

    @Column("severity")
    private String severity;

    @Column("metadata")
    private Map<String, String> metadata;

    @Column("correlation_id")
    private String correlationId;

    @Column("tags")
    private List<String> tags;

    @Column("raw_payload")
    private String rawPayload;
}
