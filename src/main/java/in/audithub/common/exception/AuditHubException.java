package in.audithub.common.exception;

public class AuditHubException extends RuntimeException {
    public AuditHubException(String message) {
        super(message);
    }
    public AuditHubException(String message, Throwable cause) {
        super(message, cause);
    }
}
