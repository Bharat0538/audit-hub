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
@Table("entity_timeline")
public class EntityTimeline {

    @PrimaryKey
    private EntityTimelineKey key;

    @Column("actor_user_id")
    private String actorUserId;

    @Column("actor_user_name")
    private String actorUserName;

    @Column("action_type")
    private String actionType;

    @Column("action_name")
    private String actionName;

    @Column("action_description")
    private String actionDescription;

    @Column("changes")
    private String changes;

    @Column("outcome")
    private String outcome;

    @Column("severity")
    private String severity;
}
