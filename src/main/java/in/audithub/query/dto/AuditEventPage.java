package in.audithub.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventPage<T> {
    private List<T> content;
    private String pageToken;
    private boolean hasMore;
    private Long totalEstimate;

    public static <T> AuditEventPage<T> of(List<T> content, String pageToken, boolean hasMore) {
        return AuditEventPage.<T>builder()
                .content(content)
                .pageToken(pageToken)
                .hasMore(hasMore)
                .build();
    }

    public static <T> AuditEventPage<T> of(List<T> content, String pageToken, boolean hasMore, Long totalEstimate) {
        return AuditEventPage.<T>builder()
                .content(content)
                .pageToken(pageToken)
                .hasMore(hasMore)
                .totalEstimate(totalEstimate)
                .build();
    }
}
