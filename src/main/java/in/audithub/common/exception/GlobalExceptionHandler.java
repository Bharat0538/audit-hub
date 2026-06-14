package in.audithub.common.exception;

import in.audithub.common.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleQuotaExceeded(QuotaExceededException ex) {
        String traceId = generateTraceId();
        log.warn("Quota exceeded: {}. Trace ID: {}", ex.getMessage(), traceId);
        ApiResponse<Void> response = ApiResponse.error("QUOTA_EXCEEDED", ex.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body(response);
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantNotFound(TenantNotFoundException ex) {
        String traceId = generateTraceId();
        log.warn("Tenant not found: {}. Trace ID: {}", ex.getMessage(), traceId);
        ApiResponse<Void> response = ApiResponse.error("TENANT_NOT_FOUND", ex.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        String traceId = generateTraceId();
        log.warn("Unauthorized access: {}. Trace ID: {}", ex.getMessage(), traceId);
        ApiResponse<Void> response = ApiResponse.error("UNAUTHORIZED", ex.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String traceId = generateTraceId();
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failure: {}. Trace ID: {}", errors, traceId);
        ApiResponse<Void> response = ApiResponse.error("INVALID_INPUT", "Validation failed", errors, traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        String traceId = generateTraceId();
        log.error("Internal Server Error: {}. Trace ID: {}", ex.getMessage(), traceId, ex);
        ApiResponse<Void> response = ApiResponse.error("INTERNAL_SERVER_ERROR", "An unexpected error occurred", traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}
