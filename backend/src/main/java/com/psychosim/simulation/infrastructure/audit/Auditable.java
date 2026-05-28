package com.psychosim.simulation.infrastructure.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for automatic audit trail recording.
 *
 * <p>The {@link SimulationAuditAspect} intercepts annotated methods and persists an
 * entry to the {@code audit_logs} table after the method returns successfully.
 * The audit record includes actor identity (from {@code SecurityContextHolder}),
 * IP address / User-Agent (from {@code RequestContextHolder}), and the resolved
 * resource identifier extracted from method arguments.
 *
 * <h3>Usage example:</h3>
 * <pre>
 * {@literal @}Auditable(action = "ADMIN_CREATE_NODE", resourceType = "CASE_VERSION")
 * public CaseEditorView createNode(Long caseVersionId, NodeUpsertRequest request) { ... }
 *
 * {@literal @}Auditable(action = "ADMIN_UPDATE_NODE", resourceType = "NODE", resourceIdParamIndex = 1)
 * public CaseEditorView updateNode(Long caseVersionId, Long nodeId, NodeUpsertRequest request) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /** Audit action identifier (e.g. {@code "ADMIN_CREATE_NODE"}, {@code "DECISION_SELECTED"}). */
    String action();

    /** Domain resource type being acted on (e.g. {@code "NODE"}, {@code "ATTEMPT"}). */
    String resourceType() default "";

    /**
     * Zero-based index of the method parameter whose {@code toString()} value is used
     * as {@code resource_id} in the audit record.  Defaults to {@code 0} (first parameter).
     * Use {@code -1} to suppress resource ID capture.
     */
    int resourceIdParamIndex() default 0;
}
