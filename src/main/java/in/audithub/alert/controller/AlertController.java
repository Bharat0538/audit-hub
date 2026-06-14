package in.audithub.alert.controller;

import in.audithub.alert.model.AlertRule;
import in.audithub.alert.service.AlertRuleService;
import in.audithub.common.model.ApiResponse;
import in.audithub.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/alerts/rules")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRuleService ruleService;
    private final TenantContext tenantContext;

    @PostMapping
    public ApiResponse<AlertRule> createRule(@RequestBody AlertRule rule) {
        UUID orgId = tenantContext.getOrganizationId();
        rule.setOrganizationId(orgId);
        // Set a dummy user ID for now since we're testing via integration scripts
        rule.setCreatedBy(UUID.randomUUID());
        AlertRule created = ruleService.createRule(rule);
        return ApiResponse.success(created);
    }

    @GetMapping
    public ApiResponse<List<AlertRule>> getRules() {
        UUID orgId = tenantContext.getOrganizationId();
        List<AlertRule> rules = ruleService.getRulesByOrg(orgId);
        return ApiResponse.success(rules);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable UUID id) {
        ruleService.deleteRule(id);
        return ApiResponse.success(null);
    }
}
