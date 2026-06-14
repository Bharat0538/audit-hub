package in.audithub.ingestion.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AuditEventBatchRequest {
    @NotNull(message = "Events list is required")
    @Size(min = 1, max = 1000, message = "Batch size must be between 1 and 1000")
    @Valid
    private List<AuditEventRequest> events;
}
