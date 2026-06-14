package in.audithub.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventSearchRequest {
    private UUID organizationId;
    private UUID applicationId;
    private Instant startTime;
    private Instant endTime;
    private List<String> actionTypes;
    private List<String> severities;
    private String actorUserId;
    private String resourceId;
    private int pageSize;
    private String pageToken;
}
