package in.audithub.sdk.context;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SpringSecurityAuditContextProvider implements AuditContextProvider {

    @Override
    public AuditContext current() {
        String userId = "system";
        String userEmail = null;
        String userName = null;
        String ipAddress = "127.0.0.1";
        String userAgent = null;
        String sessionId = null;
        String correlationId = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            userId = auth.getName();
            // In a real application, user details can be fetched from auth principal
            userEmail = userId + "@example.com"; 
            userName = userId;
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest req = attributes.getRequest();
            userAgent = req.getHeader("User-Agent");
            correlationId = req.getHeader("X-Correlation-ID");
            
            // Resolve IP Address
            String xForwardedFor = req.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                ipAddress = xForwardedFor.split(",")[0].trim();
            } else {
                ipAddress = req.getRemoteAddr();
            }

            if (req.getSession(false) != null) {
                sessionId = req.getSession(false).getId();
            }
        }

        return AuditContext.builder()
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .sessionId(sessionId)
                .correlationId(correlationId)
                .build();
    }
}
