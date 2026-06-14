package in.audithub.ingestion.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventRequest {
    private UUID eventId;
    private UUID applicationId;
    private Instant eventTime;

    @NotNull(message = "Actor details are required")
    @Valid
    private Actor actor;

    @NotNull(message = "Action details are required")
    @Valid
    private Action action;

    @NotNull(message = "Resource details are required")
    @Valid
    private Resource resource;

    @Valid
    private List<FieldChange> changes;

    @Builder.Default
    private Outcome outcome = Outcome.SUCCESS;

    @Builder.Default
    private Severity severity = Severity.LOW;

    private String correlationId;
    @jakarta.validation.constraints.Size(max = 50, message = "Maximum 50 metadata keys allowed")
    private Map<
        @jakarta.validation.constraints.Size(max = 100, message = "Metadata key max 100 characters") String,
        @jakarta.validation.constraints.Size(max = 500, message = "Metadata value max 500 characters") String
    > metadata;

    @jakarta.validation.constraints.Size(max = 20, message = "Maximum 20 tags allowed")
    private List<
        @jakarta.validation.constraints.Size(max = 100)
        @jakarta.validation.constraints.Pattern(regexp = "^[a-z0-9-_]+$", message = "Tags must be lowercase alphanumeric with hyphens or underscores") String
    > tags;

    @jakarta.validation.constraints.AssertTrue(message = "Field change values must not exceed 10KB")
    public boolean isChangesValid() {
        if (changes == null) return true;
        return changes.stream().allMatch(c ->
            (c.getOldValue() == null || c.getOldValue().toString().length() < 10240) &&
            (c.getNewValue() == null || c.getNewValue().toString().length() < 10240)
        );
    }
}
