package com.psychosim.simulation.infrastructure.audit;

import com.psychosim.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP aspect that intercepts all methods annotated with {@link Auditable} and
 * persists an audit record via {@link AuditLogAdapter} after the join-point returns
 * successfully.
 *
 * <p>Audit is written in a separate transaction ({@code REQUIRES_NEW} inside the
 * adapter) so it is committed independently of the calling transaction.
 *
 * <p>All internal failures are caught and logged; they must never propagate to callers.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SimulationAuditAspect {

    private final AuditLogAdapter auditLogAdapter;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        // Always execute the real operation first
        Object result = pjp.proceed();

        try {
            writeAudit(pjp, auditable);
        } catch (Exception ex) {
            log.warn("Audit aspect error [action={}]: {}", auditable.action(), ex.getMessage(), ex);
        }

        return result;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private void writeAudit(ProceedingJoinPoint pjp, Auditable auditable) {
        // 1. Actor identity from SecurityContextHolder
        String actorId = null;
        String actorRole = "ANONYMOUS";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            if (user.getId() != null) {
                actorId = user.getId().toString();
            }
            if (user.getRole() != null) {
                actorRole = user.getRole().name();
            }
        } else if (auth != null && auth.isAuthenticated()) {
            actorRole = "SYSTEM";
        }

        // 2. Resource ID from method parameter
        String resourceId = null;
        int idx = auditable.resourceIdParamIndex();
        Object[] args = pjp.getArgs();
        if (idx >= 0 && args != null && idx < args.length && args[idx] != null) {
            resourceId = args[idx].toString();
        }

        // 3. HTTP context (null-safe: not present during unit tests)
        String ipAddress = null;
        String userAgent = null;
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            ipAddress = resolveClientIp(req);
            userAgent = req.getHeader("User-Agent");
        }

        // 4. Extra context map
        Map<String, String> context = new HashMap<>();
        context.put("method", pjp.getSignature().getName());
        context.put("class", pjp.getTarget().getClass().getSimpleName());

        // 5. Persist
        auditLogAdapter.append(
                actorId,
                actorRole,
                auditable.action(),
                auditable.resourceType().isBlank() ? null : auditable.resourceType(),
                resourceId,
                context,
                ipAddress,
                userAgent,
                Instant.now()
        );
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // First IP in the chain is the originating client
            return xff.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
