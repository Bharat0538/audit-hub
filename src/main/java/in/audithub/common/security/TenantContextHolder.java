package in.audithub.common.security;

import java.util.UUID;

public class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = ThreadLocal.withInitial(TenantContext::new);

    public static void set(UUID organizationId, UUID applicationId) {
        TenantContext ctx = CONTEXT.get();
        ctx.setOrganizationId(organizationId);
        ctx.setApplicationId(applicationId);
    }

    public static void setUserId(UUID userId) {
        CONTEXT.get().setUserId(userId);
    }

    public static TenantContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static class TenantContext {
        private UUID organizationId;
        private UUID applicationId;
        private UUID userId;

        public UUID getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(UUID organizationId) {
            this.organizationId = organizationId;
        }

        public UUID getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(UUID applicationId) {
            this.applicationId = applicationId;
        }

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }
    }
}
