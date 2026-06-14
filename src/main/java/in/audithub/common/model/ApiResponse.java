package in.audithub.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ApiError error;
    private String traceId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiError {
        private String code;
        private String message;
        private Object details;
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .traceId(traceId)
                .build();
    }

    public static ApiResponse<Void> error(String code, String message, String traceId) {
        return ApiResponse.<Void>builder()
                .success(false)
                .error(ApiError.builder().code(code).message(message).build())
                .traceId(traceId)
                .build();
    }

    public static ApiResponse<Void> error(String code, String message, Object details, String traceId) {
        return ApiResponse.<Void>builder()
                .success(false)
                .error(ApiError.builder().code(code).message(message).details(details).build())
                .traceId(traceId)
                .build();
    }
}
