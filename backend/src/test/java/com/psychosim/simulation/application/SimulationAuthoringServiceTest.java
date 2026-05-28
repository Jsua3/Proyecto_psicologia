package com.psychosim.simulation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psychosim.domain.user.User;
import com.psychosim.domain.user.UserRepository;
import com.psychosim.domain.user.UserRole;
import com.psychosim.simulation.domain.model.CasePublicationStatus;
import com.psychosim.simulation.infrastructure.persistence.CaseVersionEntity;
import com.psychosim.simulation.infrastructure.persistence.CaseVersionJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SimulationCaseEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationCaseJpaRepository;
import com.psychosim.simulation.web.SimulationDtos.CaseEditorView;
import com.psychosim.simulation.web.SimulationDtos.ChecklistUpdateRequest;
import com.psychosim.simulation.web.SimulationDtos.DecisionOptionUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.NodeUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.ToolUpsertRequest;
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

/**
 * Pruebas de integración de SimulationAuthoringService con H2 en memoria.
 * Verifica el CRUD de nodos, decisiones, herramientas y checklist.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimulationAuthoringServiceTest {

    @Autowired private SimulationAuthoringService authoringService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SimulationCaseJpaRepository caseRepository;
    @Autowired private CaseVersionJpaRepository versionRepository;

    private Long caseVersionId;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Persistir usuario admin de prueba
        adminUser = new User();
        adminUser.setEmail("admin-authoring@psychosim.edu.co");
        adminUser.setPasswordHash(passwordEncoder.encode("Admin123!"));
        adminUser.setNombre("Admin");
        adminUser.setApellido("Authoring");
        adminUser.setRole(UserRole.ADMIN);
        adminUser = userRepository.save(adminUser);

        // Crear caso y versión de prueba
        SimulationCaseEntity caseEntity = new SimulationCaseEntity();
        caseEntity.setCode("CRUD-TEST-" + System.currentTimeMillis());
        caseEntity.setTitle("Caso de prueba CRUD");
        caseEntity.setDescription("Caso usado en pruebas de autoría");
        caseEntity.setCreatedBy(adminUser);
        caseEntity = caseRepository.save(caseEntity);

        CaseVersionEntity version = new CaseVersionEntity();
        version.setSimulationCase(caseEntity);
        version.setSemanticVersion("1.0.0");
        version.setStatus(CasePublicationStatus.DRAFT);
        version.setNarrativeContext("Contexto de prueba");
        version.setCreatedBy(adminUser);
        version.setCreatedAt(LocalDateTime.now());
        version = versionRepository.save(version);
        caseVersionId = version.getId();
    }

    // ─── Editor view ─────────────────────────────────────────────────────────────

    @Test
    void editor_version_vacia_devuelve_listas_vacias() {
        CaseEditorView result = authoringService.editor(caseVersionId);

        assertThat(result.caseVersionId()).isEqualTo(caseVersionId);
        assertThat(result.nodes()).isEmpty();
        assertThat(result.decisions()).isEmpty();
        assertThat(result.maps()).isEmpty();
        assertThat(result.objects()).isEmpty();
        assertThat(result.checklistCompletion()).isZero();
        assertThat(result.publishable()).isFalse();
    }

    // ─── Nodo CRUD ───────────────────────────────────────────────────────────────

    @Test
    void crear_nodo_devuelve_editor_con_nodo() {
        NodeUpsertRequest request = new NodeUpsertRequest(
                "nodo-test", "Nodo de prueba", "Narrativa de prueba",
                List.of(), List.of(),
                false, false, null, false, true, 100, 50
        );

        CaseEditorView result = authoringService.createNode(caseVersionId, request);

        assertThat(result.nodes()).hasSize(1);
        assertThat(result.nodes().get(0).key()).isEqualTo("nodo-test");
        assertThat(result.nodes().get(0).startNode()).isTrue();
        assertThat(result.nodes().get(0).positionX()).isEqualTo(100);
    }

    @Test
    void actualizar_nodo_modifica_campos() {
        NodeUpsertRequest create = new NodeUpsertRequest(
                "nodo-v1", "Título v1", "Narrativa v1",
                List.of(), List.of(), false, false, null, false, true, null, null
        );
        Long nodeId = authoringService.createNode(caseVersionId, create).nodes().get(0).id();

        NodeUpsertRequest update = new NodeUpsertRequest(
                "nodo-v2", "Título v2", "Narrativa v2",
                List.of("PAP"), List.of(), true, false, "Advertencia", false, true, 200, 100
        );
        CaseEditorView result = authoringService.updateNode(caseVersionId, nodeId, update);

        assertThat(result.nodes().get(0).key()).isEqualTo("nodo-v2");
        assertThat(result.nodes().get(0).sensitiveContent()).isTrue();
        assertThat(result.nodes().get(0).positionX()).isEqualTo(200);
    }

    @Test
    void eliminar_nodo_lo_quita_del_editor() {
        NodeUpsertRequest request = new NodeUpsertRequest(
                "nodo-borrar", "Borrar", "Narrativa",
                List.of(), List.of(), false, false, null, false, true, null, null
        );
        Long nodeId = authoringService.createNode(caseVersionId, request).nodes().get(0).id();

        CaseEditorView result = authoringService.deleteNode(caseVersionId, nodeId);

        assertThat(result.nodes()).isEmpty();
    }

    // ─── Decision CRUD ────────────────────────────────────────────────────────────

    @Test
    void crear_decision_entre_dos_nodos_aparece_como_arista() {
        Long srcId = authoringService.createNode(caseVersionId, new NodeUpsertRequest(
                "src", "Origen", "Narrativa", List.of(), List.of(),
                false, false, null, false, true, null, null
        )).nodes().get(0).id();

        Long dstId = authoringService.createNode(caseVersionId, new NodeUpsertRequest(
                "dst", "Destino", "Narrativa", List.of(), List.of(),
                false, false, null, true, false, null, null
        )).nodes().stream().filter(n -> n.key().equals("dst")).findFirst().get().id();

        DecisionOptionUpsertRequest decReq = new DecisionOptionUpsertRequest(
                srcId, dstId, "dec-test",
                "Escucha activa", "ADEQUATE", false, null,
                10, 0, 0, "Muy bien"
        );
        CaseEditorView result = authoringService.createDecision(caseVersionId, decReq);

        assertThat(result.decisions()).hasSize(1);
        assertThat(result.decisions().get(0).sourceKey()).isEqualTo("src");
        assertThat(result.decisions().get(0).targetKey()).isEqualTo("dst");
        assertThat(result.decisions().get(0).classification()).isEqualTo("ADEQUATE");
    }

    // ─── Herramienta CRUD ────────────────────────────────────────────────────────

    @Test
    void crear_herramienta_aparece_en_editor() {
        ToolUpsertRequest req = new ToolUpsertRequest(
                "PAP-T", "PAP de prueba", "psychology", "clinical", "Descripción PAP de prueba."
        );
        CaseEditorView result = authoringService.createTool(caseVersionId, req);

        assertThat(result.tools()).hasSize(1);
        assertThat(result.tools().get(0).code()).isEqualTo("PAP-T");
    }

    @Test
    void eliminar_herramienta_la_quita_del_editor() {
        Long toolId = authoringService.createTool(caseVersionId,
                new ToolUpsertRequest("T-DEL", "Borrar", "psychology", "clinical", "Desc.")
        ).tools().get(0).id();

        CaseEditorView result = authoringService.deleteTool(caseVersionId, toolId);

        assertThat(result.tools()).isEmpty();
    }

    // ─── Checklist ────────────────────────────────────────────────────────────────

    @Test
    void checklist_parcial_no_habilita_publicacion() {
        CaseEditorView result = authoringService.updateChecklist(caseVersionId,
                new ChecklistUpdateRequest(true, true, false, false, false, false), adminUser);

        assertThat(result.publishable()).isFalse();
        assertThat(result.checklistCompletion()).isLessThan(100);
    }

    @Test
    void checklist_completo_habilita_publicacion() {
        CaseEditorView result = authoringService.updateChecklist(caseVersionId,
                new ChecklistUpdateRequest(true, true, true, true, true, true), adminUser);

        assertThat(result.checklistCompletion()).isEqualTo(100);
        assertThat(result.publishable()).isTrue();
    }
}
