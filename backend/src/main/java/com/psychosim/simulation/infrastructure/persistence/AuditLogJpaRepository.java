package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Long> {

    /** Used by the cleanup scheduler to purge entries past their 12-month retention date. */
    @Modifying
    @Query("DELETE FROM AuditLogEntity a WHERE a.retentionUntil < :cutoff")
    int deleteByRetentionUntilBefore(@Param("cutoff") Instant cutoff);

    /** Retrieve audit trail for a specific actor (e.g. instructor reviewing a student). */
    List<AuditLogEntity> findByActorIdOrderByOccurredAtDesc(Long actorId);

    /** Retrieve audit trail for a specific resource type and resource id. */
    List<AuditLogEntity> findByResourceTypeAndResourceIdOrderByOccurredAtDesc(
            String resourceType, String resourceId);
}
