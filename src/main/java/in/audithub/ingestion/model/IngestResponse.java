package in.audithub.ingestion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestResponse {
    private UUID eventId;
    private IngestStatus status;
    private String message;

    public static IngestResponse accepted(UUID eventId) {
        return IngestResponse.builder()
                .eventId(eventId)
                .status(IngestStatus.ACCEPTED)
                .message("Event accepted for processing")
                .build();
    }

    public static IngestResponse duplicate(UUID eventId) {
        return IngestResponse.builder()
                .eventId(eventId)
                .status(IngestStatus.DUPLICATE)
                .message("Duplicate event detected")
                .build();
    }
}
