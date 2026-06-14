package in.audithub.common.security;

import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class TenantContext {

    public UUID getOrganizationId() {
        return TenantContextHolder.get().getOrganizationId();
    }

    public UUID getApplicationId() {
        return TenantContextHolder.get().getApplicationId();
    }

    public UUID getUserId() {
        return TenantContextHolder.get().getUserId();
    }
}
