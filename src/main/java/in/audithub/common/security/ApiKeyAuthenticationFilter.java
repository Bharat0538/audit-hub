package in.audithub.common.security;

import in.audithub.tenant.service.ApiKeyService;
import in.audithub.tenant.service.ApiKeyService.ApiKeyPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || !apiKey.startsWith("ah_")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            ApiKeyPrincipal principal = apiKeyService.validateAndLoad(apiKey);

            TenantContextHolder.set(principal.getOrganizationId(), principal.getApplicationId());

            var authorities = principal.getScopes().stream()
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated request using API key for org: {}", principal.getOrganizationId());

        } catch (Exception e) {
            log.warn("API key authentication failed: {}", e.getMessage());
            // Clear context and let standard security handle failures
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
