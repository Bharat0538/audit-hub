package in.audithub.common.exception;

public class TenantNotFoundException extends AuditHubException {
    public TenantNotFoundException(String message) {
        super(message);
    }
}
