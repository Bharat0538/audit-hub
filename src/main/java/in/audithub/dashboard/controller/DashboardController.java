package in.audithub.dashboard.controller;

import in.audithub.common.model.ApiResponse;
import in.audithub.common.security.TenantContext;
import in.audithub.dashboard.dto.DashboardStats;
import in.audithub.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final TenantContext tenantContext;

    @GetMapping("/stats")
    public ApiResponse<DashboardStats> getStats(@RequestParam(required = false) UUID applicationId) {
        UUID orgId = tenantContext.getOrganizationId();
        DashboardStats stats = dashboardService.getDashboardStats(orgId, applicationId);
        return ApiResponse.success(stats);
    }

    @GetMapping("/trends")
    public ApiResponse<java.util.Map<String, Object>> getTrends(
            @RequestParam(required = false) UUID applicationId,
            @RequestParam(required = false) String granularity,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        UUID orgId = tenantContext.getOrganizationId();
        java.util.Map<String, Object> trends = dashboardService.getTrends(orgId, applicationId, startTime, endTime);
        return ApiResponse.success(trends);
    }
}
