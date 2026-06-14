package in.audithub.ingestion.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Actor {
    @NotBlank(message = "Actor userId is required")
    private String userId;
    private String userEmail;
    private String userName;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
}
