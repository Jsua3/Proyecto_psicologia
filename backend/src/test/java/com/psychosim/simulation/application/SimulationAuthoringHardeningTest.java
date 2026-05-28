package com.psychosim.simulation.application;

import com.psychosim.domain.user.User;
import com.psychosim.domain.user.UserRepository;
import com.psychosim.domain.user.UserRole;
import com.psychosim.simulation.domain.model.CasePublicationStatus;
import com.psychosim.simulation.infrastructure.persistence.CaseVersionEntity;
import com.psychosim.simulation.infrastructure.persistence.CaseVersionJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SimulationCaseEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationCaseJpaRepository;
import com.psychosim.simulation.web.SimulationDtos.NodeUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.ToolUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.WorldValidationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración de blindaje del editor:
 * - Guard ensureDraft: PUBLISHED no puede mutarse.
 * - Clonación desde PUBLISHED produce DRAFT nuevo.
 * - Pertenencia cruzada rechazada.
 * - validateWorld devuelve estado de validación coherente.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimulationAuthoringHardeningTest {

    @Autowired private SimulationAuthoringService authoringService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SimulationCaseJpaRepository caseRepository;
    @Autowired private CaseVersionJpaRepository versionRepository;

    private Long draftVersionId;
    private Long publishedVersionId;
    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setEmail("hardening-admin@psychosim.edu.co");
        adminUser.setPasswordHash(passwordEncoder.encode("Admin123!"));
        adminUser.setNombre("Hardening");
        adminUser.setApellido("Admin");
        adminUser.setRole(UserRole.ADMIN);
        adminUser = userRepository.save(adminUser);

        SimulationCaseEntity caso = new SimulationCaseEntity();
        caso.setCode("HARD-" + System.currentTimeMillis());
        caso.setTitle("Caso Hardening");
        caso.setDescription("Para pruebas de blindaje");
        caso.setCreatedBy(adminUser);
        caso = caseRepository.save(caso);

        // Versión DRAFT
        CaseVersionEntity draft = new CaseVersionEntity();
        draft.setSimulationCase(caso);
        draft.setSemanticVersion("1.0.0");
        draft.setStatus(CasePublicationStatus.DRAFT);
        draft.setNarrativeContext("Contexto draft");
        draft.setCreatedBy(adminUser);
        draft.setCreatedAt(LocalDateTime.now());
        draft = versionRepository.save(draft);
        draftVersionId = draft.getId();

        // Versión PUBLISHED (persistida directamente para evitar validaciones del servicio)
        CaseVersionEntity published = new CaseVersionEntity();
        published.setSimulationCase(caso);
        published.setSemanticVersion("0.9.0");
        published.setStatus(CasePublicationStatus.PUBLISHED);
        published.setNarrativeContext("Contexto publicado");
        published.setCreatedBy(adminUser);
        published.setCreatedAt(LocalDateTime.now());
        published.setPublishedAt(LocalDateTime.now());
        published = versionRepository.save(published);
        publishedVersionId = published.getId();
    }

    // ─── Guard ensureDraft ─────────────────────────────────────────────────────

    @Test
    void crear_nodo_en_version_published_lanza_IllegalStateException() {
        NodeUpsertRequest req = new NodeUpsertRequest(
                "nodo-prohibido", "No debe crearse", "Narrativa",
                List.of(), List.of(), false, false, null, false, true, 0, 0);

        assertThatThrownBy(() -> authoringService.createNode(publishedVersionId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void crear_herramienta_en_version_published_lanza_IllegalStateException() {
        ToolUpsertRequest req = new ToolUpsertRequest(
                "PAP", "Primera Ayuda Psicológica", "psychology", "clinical", "Desc.");

        assertThatThrownBy(() -> authoringService.createTool(publishedVersionId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void crear_nodo_en_version_draft_no_lanza() {
        NodeUpsertRequest req = new NodeUpsertRequest(
                "nodo-draft-ok", "OK", "Narrativa",
                List.of(), List.of(), false, false, null, false, true, 0, 0);

        // No debe lanzar
        assertThat(authoringService.createNode(draftVersionId, req).nodes()).hasSize(1);
    }

    // ─── Clonación desde PUBLISHED ────────────────────────────────────────────

    @Test
    void clonar_version_published_produce_nuevo_draft() {
        var cloned = authoringService.cloneVersion(publishedVersionId, adminUser);

        assertThat(cloned.status()).isEqualTo("DRAFT");
        assertThat(cloned.caseVersionId()).isNotEqualTo(publishedVersionId);
    }

    @Test
    void clonar_version_draft_produce_nuevo_draft() {
        var cloned = authoringService.cloneVersion(draftVersionId, adminUser);

        assertThat(cloned.status()).isEqualTo("DRAFT");
        assertThat(cloned.caseVersionId()).isNotEqualTo(draftVersionId);
    }

    // ─── Pertenencia cruzada ──────────────────────────────────────────────────

    @Test
    void actualizar_nodo_de_otra_version_lanza_exception() {
        // Crear un nodo en la versión DRAFT
        NodeUpsertRequest req = new NodeUpsertRequest(
                "nodo-draft", "Nodo Draft", "Narrativa",
                List.of(), List.of(), false, false, null, false, true, 0, 0);
        Long nodeId = authoringService.createNode(draftVersionId, req).nodes().get(0).id();

        // Clonar para tener otra versión
        Long otherVersionId = authoringService.cloneVersion(draftVersionId, adminUser).caseVersionId();

        // Intentar actualizar el nodo de la versión draft usando el id de la otra versión
        NodeUpsertRequest updateReq = new NodeUpsertRequest(
                "nodo-draft-modificado", "Modificado", "Narrativa modificada",
                List.of(), List.of(), false, false, null, false, true, 10, 20);

        assertThatThrownBy(() -> authoringService.updateNode(otherVersionId, nodeId, updateReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");
    }

    // ─── validateWorld ────────────────────────────────────────────────────────

    @Test
    void validateWorld_version_sin_nodos_reporta_errores() {
        WorldValidationState state = authoringService.validateWorld(draftVersionId);

        assertThat(state.canPublish()).isFalse();
        assertThat(state.errors()).isNotEmpty();
        // Debe haber error de "sin nodos" o "sin nodo inicial"
        assertThat(state.errors())
                .anyMatch(i -> i.code().equals("NO_NODES") || i.code().equals("NO_START_NODE"));
    }

    @Test
    void validateWorld_version_published_tambien_valida_sin_mutar() {
        // validateWorld es readOnly y no requiere DRAFT
        WorldValidationState state = authoringService.validateWorld(publishedVersionId);

        // La versión publicada tampoco tiene nodos en este test → reporta errores
        assertThat(state).isNotNull();
        assertThat(state.errors()).isNotEmpty();
    }
}
