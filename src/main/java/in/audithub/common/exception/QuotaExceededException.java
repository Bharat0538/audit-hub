package in.audithub.common.exception;

public class QuotaExceededException extends AuditHubException {
    public QuotaExceededException(String message) {
        super(message);
    }
}
