package in.audithub.sdk.context;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditContext {
    private String userId;
    private String userEmail;
    private String userName;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    private String correlationId;
}
