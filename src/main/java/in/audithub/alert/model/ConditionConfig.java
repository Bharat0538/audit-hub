package in.audithub.alert.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionConfig {
    private String actionType;
    private String resourceType;
    private int threshold;
    private int windowMinutes;
    private String groupBy; // e.g. "actor_user_id"
}
