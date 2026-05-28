package com.psychosim.simulation.infrastructure.audit;

import com.psychosim.simulation.infrastructure.persistence.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled job that purges {@code audit_logs} rows whose {@code retention_until}
 * timestamp has passed (12-month retention policy).
 *
 * <p>Runs daily at 03:00 UTC to avoid overlapping with peak-hours traffic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogCleanupScheduler {

    private final AuditLogJpaRepository auditLogRepository;

    /**
     * Purge expired audit log entries.
     * Cron expression: {@code "0 0 3 * * *"} = every day at 03:00:00 UTC.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredLogs() {
        Instant now = Instant.now();
        int deleted = auditLogRepository.deleteByRetentionUntilBefore(now);
        if (deleted > 0) {
            log.info("Audit retention purge: deleted {} expired entries (before {})", deleted, now);
        } else {
            log.debug("Audit retention purge: no entries expired as of {}", now);
        }
    }
}
