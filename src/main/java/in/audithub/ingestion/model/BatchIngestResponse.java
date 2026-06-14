package in.audithub.ingestion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchIngestResponse {
    private int accepted;
    private int duplicates;
    private int failed;
    private List<Result> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private int index;
        private UUID eventId;
        private IngestStatus status;
        private String error;

        public static Result success(int index, UUID eventId, IngestStatus status) {
            return Result.builder()
                    .index(index)
                    .eventId(eventId)
                    .status(status)
                    .build();
        }

        public static Result failure(int index, String error) {
            return Result.builder()
                    .index(index)
                    .error(error)
                    .build();
        }
    }
}
