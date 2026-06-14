package in.audithub.sdk.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.audithub.ingestion.model.*;
import in.audithub.sdk.annotation.Audited;
import in.audithub.sdk.annotation.AuditResourceId;
import in.audithub.sdk.client.AuditHubClient;
import in.audithub.sdk.context.AuditContext;
import in.audithub.sdk.context.AuditContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Aspect
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditHubClient auditHubClient;
    private final AuditContextProvider contextProvider;
    private final ObjectMapper objectMapper;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        Object result = null;
        Throwable error = null;

        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            try {
                AuditContext ctx = contextProvider.current();
                AuditEventRequest event = buildEvent(pjp, audited, ctx, result, error);
                auditHubClient.send(event);
            } catch (Exception e) {
                // Never let audit failure break business logic
                log.error("AuditHub SDK error during aspect processing: {}", e.getMessage(), e);
            }
        }
    }

    private AuditEventRequest buildEvent(ProceedingJoinPoint pjp, Audited audited,
                                         AuditContext ctx, Object result, Throwable error) {
        String resourceId = extractResourceId(pjp);

        Map<String, String> metadata = new HashMap<>();
        if (audited.captureArgs()) {
            addArgsToMetadata(pjp, metadata);
        }
        if (audited.captureResult() && result != null) {
            try {
                metadata.put("result", objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                metadata.put("result", result.toString());
            }
        }

        Actor actor = Actor.builder()
                .userId(ctx.getUserId())
                .userEmail(ctx.getUserEmail())
                .userName(ctx.getUserName())
                .ipAddress(ctx.getIpAddress())
                .userAgent(ctx.getUserAgent())
                .sessionId(ctx.getSessionId())
                .build();

        ActionType actionType;
        try {
            actionType = ActionType.valueOf(audited.actionType());
        } catch (Exception e) {
            actionType = ActionType.CUSTOM;
        }

        Action action = Action.builder()
                .type(actionType)
                .name(audited.action())
                .description(audited.description())
                .build();

        Resource resource = Resource.builder()
                .type(audited.resourceType())
                .id(resourceId)
                .build();

        Severity severity;
        try {
            severity = Severity.valueOf(audited.severity());
        } catch (Exception e) {
            severity = Severity.LOW;
        }

        List<FieldChange> changes = detectChanges(pjp);

        return AuditEventRequest.builder()
                .actor(actor)
                .action(action)
                .resource(resource)
                .outcome(error == null ? Outcome.SUCCESS : Outcome.FAILURE)
                .severity(severity)
                .correlationId(ctx.getCorrelationId())
                .metadata(metadata)
                .changes(changes)
                .eventTime(Instant.now())
                .build();
    }

    private List<FieldChange> detectChanges(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();
        Object[] args = pjp.getArgs();
        Object before = null;
        Object after = null;
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof in.audithub.sdk.annotation.AuditBefore) {
                    if (args != null && i < args.length) {
                        before = args[i];
                    }
                } else if (annotation instanceof in.audithub.sdk.annotation.AuditAfter) {
                    if (args != null && i < args.length) {
                        after = args[i];
                    }
                }
            }
        }
        if (before != null || after != null) {
            return in.audithub.sdk.util.DiffUtils.diff(before, after);
        }
        return null;
    }

    private String extractResourceId(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();
        Object[] args = pjp.getArgs();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof AuditResourceId) {
                    if (args[i] != null) {
                        return args[i].toString();
                    }
                }
            }
        }
        // Fallback: search for parameter named "id" or "resourceId"
        String[] parameterNames = signature.getParameterNames();
        if (parameterNames != null && args != null) {
            for (int i = 0; i < Math.min(parameterNames.length, args.length); i++) {
                if ("id".equalsIgnoreCase(parameterNames[i]) || "resourceId".equalsIgnoreCase(parameterNames[i])) {
                    if (args[i] != null) {
                        return args[i].toString();
                    }
                }
            }
        }
        return "unknown";
    }

    private void addArgsToMetadata(ProceedingJoinPoint pjp, Map<String, String> metadata) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = pjp.getArgs();
        if (parameterNames != null && args != null) {
            for (int i = 0; i < Math.min(parameterNames.length, args.length); i++) {
                Object arg = args[i];
                if (arg != null) {
                    try {
                        metadata.put("arg." + parameterNames[i], objectMapper.writeValueAsString(arg));
                    } catch (Exception e) {
                        metadata.put("arg." + parameterNames[i], arg.toString());
                    }
                }
            }
        }
    }
}
