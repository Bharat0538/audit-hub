package in.audithub.query.controller;

import in.audithub.common.model.ApiResponse;
import in.audithub.common.security.TenantContext;
import in.audithub.query.dto.AuditEventPage;
import in.audithub.query.dto.AuditEventResponse;
import in.audithub.query.dto.AuditEventSearchRequest;
import in.audithub.query.service.AuditQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
public class EventQueryController {

    private final AuditQueryService queryService;
    private final TenantContext tenantContext;

    @GetMapping
    public ApiResponse<AuditEventPage<AuditEventResponse>> search(
            @RequestParam(required = false) UUID applicationId,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime,
            @RequestParam(required = false) List<String> actionType,
            @RequestParam(required = false) List<String> severity,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String pageToken) {

        UUID orgId = tenantContext.getOrganizationId();
        if (startTime == null) {
            startTime = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }

        AuditEventSearchRequest request = AuditEventSearchRequest.builder()
                .organizationId(orgId)
                .applicationId(applicationId)
                .startTime(startTime)
                .endTime(endTime)
                .actionTypes(actionType)
                .severities(severity)
                .pageSize(pageSize)
                .pageToken(pageToken)
                .build();

        AuditEventPage<AuditEventResponse> result = queryService.searchEvents(request);
        return ApiResponse.success(result);
    }

    @GetMapping({"/entities/{type}/{id}", "/entity/{type}/{id}"})
    public ApiResponse<AuditEventPage<AuditEventResponse>> getEntityHistory(
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String pageToken) {

        UUID orgId = tenantContext.getOrganizationId();
        if (startTime == null) {
            startTime = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }

        AuditEventPage<AuditEventResponse> result = queryService.getEntityHistory(
                orgId, type, id, startTime, endTime, pageToken, pageSize);
        return ApiResponse.success(result);
    }

    @GetMapping({"/users/{userId}", "/user/{userId}"})
    public ApiResponse<AuditEventPage<AuditEventResponse>> getUserActivity(
            @PathVariable("userId") String userId,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String pageToken) {

        UUID orgId = tenantContext.getOrganizationId();
        if (startTime == null) {
            startTime = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }

        AuditEventPage<AuditEventResponse> result = queryService.getUserActivity(
                orgId, userId, startTime, endTime, pageToken, pageSize);
        return ApiResponse.success(result);
    }
}
