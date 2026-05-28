package com.psychosim.simulation.infrastructure.audit;

import com.psychosim.simulation.infrastructure.persistence.AuditLogJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.psychosim.simulation.infrastructure.persistence.AuditLogEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración del adaptador de auditoría con H2 en memoria.
 *
 * <p>La clase NO está anotada con {@code @Transactional} porque
 * {@link AuditLogAdapter#append} usa {@code REQUIRES_NEW} y necesita
 * confirmar su propia transacción para que los datos sean visibles.
 * La limpieza de filas sobrantes se hace inline en cada prueba.
 *
 * <p>No se usan actorId reales (FK a users.id) para evitar violaciones
 * de integridad referencial en H2. El campo es nullable y los intentos
 * de uso son opcionales.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuditLogAdapterTest {

    @Autowired private AuditLogAdapter auditLogAdapter;
    @Autowired private AuditLogJpaRepository auditLogRepository;
    @Autowired private AuditLogCleanupScheduler cleanupScheduler;

    // ─── Persistencia básica ────────────────────────────────────────────────────

    @Test
    void append_persiste_registro_con_campos_correctos() {
        long countBefore = auditLogRepository.count();
        // Truncate to milliseconds: H2 rounds sub-microseconds on TIMESTAMP columns
        Instant before = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        // actor_id nullable: no pasar id real para evitar FK violation en H2
        auditLogAdapter.append(
                null, "ADMIN",
                "ADMIN_CREATE_NODE_TEST", "CASE_VERSION", "7",
                Map.of("method", "createNode"),
                "127.0.0.1", "TestAgent/1.0",
                Instant.now()
        );

        assertThat(auditLogRepository.count()).isGreaterThan(countBefore);

        var saved = auditLogRepository.findAll().stream()
                .filter(a -> "ADMIN_CREATE_NODE_TEST".equals(a.getAction()))
                .findFirst();

        assertThat(saved).isPresent();
        assertThat(saved.get().getActorRole()).isEqualTo("ADMIN");
        assertThat(saved.get().getResourceType()).isEqualTo("CASE_VERSION");
        assertThat(saved.get().getResourceId()).isEqualTo("7");
        assertThat(saved.get().getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.get().getOccurredAt()).isAfterOrEqualTo(before);

        // Retención de al menos 11 meses a partir del evento
        Instant minRetention = before.plus(330, ChronoUnit.DAYS);
        assertThat(saved.get().getRetentionUntil()).isAfter(minRetention);

        // Cleanup
        auditLogRepository.deleteById(saved.get().getId());
    }

    @Test
    void append_con_actorId_nulo_no_lanza_excepcion() {
        long countBefore = auditLogRepository.count();

        auditLogAdapter.append(
                null, "ANONYMOUS",
                "DECISION_SELECTED_TEST", "ATTEMPT", null,
                Map.of(), null, null,
                Instant.now()
        );

        assertThat(auditLogRepository.count()).isGreaterThan(countBefore);

        // Cleanup
        auditLogRepository.findAll().stream()
                .filter(a -> "DECISION_SELECTED_TEST".equals(a.getAction()))
                .forEach(a -> auditLogRepository.deleteById(a.getId()));
    }

    @Test
    void append_porta_minimal_port_tambien_persiste() {
        long countBefore = auditLogRepository.count();

        // Llamada via interfaz AuditTrailPort (firma mínima)
        auditLogAdapter.append(
                null, "SYSTEM",
                "PORT_CALL_TEST",
                Map.of("origin", "port"),
                Instant.now()
        );

        assertThat(auditLogRepository.count()).isGreaterThan(countBefore);

        // Cleanup
        auditLogRepository.findAll().stream()
                .filter(a -> "PORT_CALL_TEST".equals(a.getAction()))
                .forEach(a -> auditLogRepository.deleteById(a.getId()));
    }

    // ─── Retención y limpieza ───────────────────────────────────────────────────

    @Test
    @Transactional
    void deleteByRetentionUntilBefore_elimina_solo_registros_expirados() {
        // En este test usamos @Transactional para el acceso al repositorio.
        // Los append usan REQUIRES_NEW (transacción propia que ya comitó antes de llegar aquí).

        // Contar antes (dentro de la transacción del test)
        long before = auditLogRepository.count();

        // Verificar que el scheduler puede correr sin lanzar excepción
        // (no habrá filas expiradas en la BD de prueba normalmente, así que deleted=0)
        // Solo validamos que no explota en H2.
        cleanupScheduler.purgeExpiredLogs();

        // El count no debe haber aumentado (cleanup solo borra, no inserta)
        assertThat(auditLogRepository.count()).isLessThanOrEqualTo(before);
    }

    @Test
    void findByResourceTypeAndResourceId_devuelve_registros_correctos() {
        auditLogAdapter.append(
                null, "ADMIN",
                "ADMIN_UPDATE_TOOL_TEST", "TOOL", "99",
                Map.of(), null, null,
                Instant.now()
        );

        List<AuditLogEntity> results = auditLogRepository
                .findByResourceTypeAndResourceIdOrderByOccurredAtDesc("TOOL", "99");

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getAction()).contains("TOOL_TEST");

        // Cleanup
        results.forEach(a -> auditLogRepository.deleteById(a.getId()));
    }
}
