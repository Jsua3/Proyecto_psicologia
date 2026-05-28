package com.psychosim.simulation.domain.service;

import com.psychosim.simulation.domain.model.WorldSnapshot;
import com.psychosim.simulation.domain.model.WorldSnapshot.CollisionSnap;
import com.psychosim.simulation.domain.model.WorldSnapshot.DecisionSnap;
import com.psychosim.simulation.domain.model.WorldSnapshot.DialogueSnap;
import com.psychosim.simulation.domain.model.WorldSnapshot.MapSnap;
import com.psychosim.simulation.domain.model.WorldSnapshot.NodeSnap;
import com.psychosim.simulation.domain.model.WorldSnapshot.ObjectSnap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias puras de WorldValidationService.
 * Sin Spring, sin JPA. Solo dominio.
 */
class WorldValidationServiceTest {

    private WorldValidationService service;

    @BeforeEach
    void setUp() {
        service = new WorldValidationService();
    }

    // ─── Grafo válido base ───────────────────────────────────────────────────────

    /** Snapshot mínimo válido: 1 start, 1 terminal, sin ciclo, geometría OK, salida segura. */
    private WorldSnapshot validSnapshot() {
        NodeSnap start = new NodeSnap(1L, true, false, 10, 10);
        NodeSnap terminal = new NodeSnap(2L, false, true, 500, 300);
        DecisionSnap edge = new DecisionSnap(1L, 1L, 2L, false, null);
        MapSnap map = new MapSnap(1L, 960, 540, 100, 100);
        ObjectSnap exitObj = new ObjectSnap(1L, 1L, 50, 50, 48, 48, "EXIT");
        return new WorldSnapshot(10L,
                List.of(start, terminal),
                List.of(edge),
                List.of(map),
                List.of(exitObj),
                List.of(),
                List.of(),
                true);
    }

    @Test
    void snapshot_valido_no_genera_errores() {
        WorldValidationResult result = service.validate(validSnapshot());
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.canPublish()).isTrue();
    }

    // ─── Grafo: nodo inicial ─────────────────────────────────────────────────────

    @Test
    void sin_nodo_inicial_genera_error_NO_START_NODE() {
        NodeSnap noStart = new NodeSnap(1L, false, true, 0, 0);
        WorldSnapshot s = new WorldSnapshot(1L, List.of(noStart), List.of(),
                List.of(new MapSnap(1L, 960, 540, 100, 100)),
                List.of(new ObjectSnap(1L, 1L, 10, 10, 48, 48, "EXIT")),
                List.of(), List.of(), true);

        WorldValidationResult result = service.validate(s);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).anySatisfy(i -> assertThat(i.code()).isEqualTo("NO_START_NODE"));
    }

    @Test
    void doble_nodo_inicial_genera_error_MULTIPLE_START_NODES() {
        NodeSnap s1 = new NodeSnap(1L, true, false, 0, 0);
        NodeSnap s2 = new NodeSnap(2L, true, true, 100, 0);
        DecisionSnap edge = new DecisionSnap(1L, 1L, 2L, false, null);
        MapSnap map = new MapSnap(1L, 960, 540, 50, 50);
        ObjectSnap exitObj = new ObjectSnap(1L, 1L, 10, 10, 48, 48, "EXIT");
        WorldSnapshot snap = new WorldSnapshot(1L,
                List.of(s1, s2), List.of(edge), List.of(map),
                List.of(exitObj), List.of(), List.of(), true);

        WorldValidationResult result = service.validate(snap);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).anySatisfy(i -> assertThat(i.code()).isEqualTo("MULTIPLE_START_NODES"));
    }

    // ─── Grafo: nodo terminal ─────────────────────────────────────────────────────

    @Test
    void sin_nodo_terminal_genera_error_NO_TERMINAL_NODE() {
        NodeSnap start = new NodeSnap(1L, true, false, 0, 0);
        WorldSnapshot s = new WorldSnapshot(1L, List.of(start), List.of(),
                List.of(new MapSnap(1L, 960, 540, 100, 100)),
                List.of(new ObjectSnap(1L, 1L, 10, 10, 48, 48, "EXIT")),
                List.of(), List.of(), true);

        WorldValidationResult result = service.validate(s);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).anySatisfy(i -> assertThat(i.code()).isEqualTo("NO_TERMINAL_NODE"));
    }

    // ─── Grafo: ciclo ─────────────────────────────────────────────────────────────

    @Test
    void ciclo_en_grafo_genera_error_GRAPH_CYCLE() {
        NodeSnap n1 = new NodeSnap(1L, true, false, 0, 0);
        NodeSnap n2 = new NodeSnap(2L, false, false, 100, 0);
        NodeSnap n3 = new NodeSnap(3L, false, true, 200, 0);
        // Ciclo: n1→n2→n1 (n3 queda desconectado pero el ciclo ya falla)
        List<DecisionSnap> decisions = List.of(
                new DecisionSnap(1L, 1L, 2L, false, null),
                new DecisionSnap(2L, 2L, 1L, false, null),  // ← ciclo
                new DecisionSnap(3L, 2L, 3L, false, null)
        );
        MapSnap map = new MapSnap(1L, 960, 540, 50, 50);
        ObjectSnap exitObj = new ObjectSnap(1L, 1L, 10, 10, 48, 48, "EXIT");
        WorldSnapshot snap = new WorldSnapshot(1L,
                List.of(n1, n2, n3), decisions, List.of(map),
                List.of(exitObj), List.of(), List.of(), true);

        WorldValidationResult result = service.validate(snap);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).anySatisfy(i -> assertThat(i.code()).isEqualTo("GRAPH_CYCLE"));
    }

    // ─── Geometría ────────────────────────────────────────────────────────────────

    @Test
    void objeto_fuera_del_mapa_genera_error_OBJECT_OUT_OF_BOUNDS() {
        WorldSnapshot base = validSnapshot();
        // Añadimos un objeto que sale del mapa (960×540)
        ObjectSnap outOfBounds = new ObjectSnap(99L, 1L, 950, 500, 48, 48, "PERSON");
        WorldSnapshot snap = new WorldSnapshot(base.caseVersionId(),
                base.nodes(), base.decisions(), base.maps(),
                List.of(base.objects().get(0), outOfBounds),
                base.collisions(), base.dialogues(), base.hasSafeExit());

        WorldValidationResult result = service.validate(snap);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).anySatisfy(i -> assertThat(i.code()).isEqualTo("OBJECT_OUT_OF_BOUNDS"));
    }

    // ─── Ética ────────────────────────────────────────────────────────────────────

    @Test
    void decision_prohibida_sin_razon_genera_error_PROHIBITED_WITHOUT_REASON() {
        WorldSnapshot base = validSnapshot();
        // Reemplazar la decisión normal por una prohibida sin razón
        DecisionSnap prohibited = new DecisionSnap(1L, 1L, 2L, true, null);
        WorldSnapshot snap = new WorldSnapshot(base.caseVersionId(),
                base.nodes(), List.of(prohibited),
                base.maps(), base.objects(), base.collisions(), base.dialogues(),
                base.hasSafeExit());

        WorldValidationResult result = service.validate(snap);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).anySatisfy(i -> assertThat(i.code()).isEqualTo("PROHIBITED_WITHOUT_REASON"));
    }

    @Test
    void decision_prohibida_con_razon_no_genera_error() {
        WorldSnapshot base = validSnapshot();
        DecisionSnap prohibited = new DecisionSnap(1L, 1L, 2L, true, "Razón documentada");
        WorldSnapshot snap = new WorldSnapshot(base.caseVersionId(),
                base.nodes(), List.of(prohibited),
                base.maps(), base.objects(), base.collisions(), base.dialogues(),
                base.hasSafeExit());

        WorldValidationResult result = service.validate(snap);
        // No debe haber errores sobre esta decisión en particular
        assertThat(result.errors())
                .noneMatch(i -> i.code().equals("PROHIBITED_WITHOUT_REASON"));
    }

    // ─── Salida segura ────────────────────────────────────────────────────────────

    @Test
    void sin_salida_segura_genera_error_NO_SAFE_EXIT() {
        WorldSnapshot base = validSnapshot();
        WorldSnapshot snap = new WorldSnapshot(base.caseVersionId(),
                base.nodes(), base.decisions(), base.maps(),
                base.objects(), base.collisions(), base.dialogues(),
                false); // hasSafeExit = false

        WorldValidationResult result = service.validate(snap);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).anySatisfy(i -> assertThat(i.code()).isEqualTo("NO_SAFE_EXIT"));
    }

    // ─── Límites ─────────────────────────────────────────────────────────────────

    @Test
    void exceso_de_objetos_genera_error_TOO_MANY_OBJECTS() {
        WorldSnapshot base = validSnapshot();
        // Generar 251 objetos en el mismo mapa (máx 250)
        List<ObjectSnap> tooMany = new java.util.ArrayList<>();
        tooMany.add(base.objects().get(0)); // EXIT
        for (int i = 1; i <= WorldValidationService.MAX_OBJECTS_PER_MAP; i++) {
            // Objetos dentro del mapa 960×540, tamaño 10×10
            tooMany.add(new ObjectSnap(100L + i, 1L, 0, 0, 10, 10, "PERSON"));
        }
        WorldSnapshot snap = new WorldSnapshot(base.caseVersionId(),
                base.nodes(), base.decisions(), base.maps(),
                tooMany, base.collisions(), base.dialogues(), base.hasSafeExit());

        WorldValidationResult result = service.validate(snap);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).anySatisfy(i -> assertThat(i.code()).isEqualTo("TOO_MANY_OBJECTS"));
    }

    @Test
    void mapa_demasiado_grande_genera_error_MAP_TOO_LARGE() {
        WorldSnapshot base = validSnapshot();
        MapSnap bigMap = new MapSnap(1L,
                WorldValidationService.MAX_MAP_WIDTH + 1,
                WorldValidationService.MAX_MAP_HEIGHT + 1,
                100, 100);
        WorldSnapshot snap = new WorldSnapshot(base.caseVersionId(),
                base.nodes(), base.decisions(), List.of(bigMap),
                base.objects(), base.collisions(), base.dialogues(), base.hasSafeExit());

        WorldValidationResult result = service.validate(snap);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).anySatisfy(i -> assertThat(i.code()).isEqualTo("MAP_TOO_LARGE"));
    }

    @Test
    void muchos_dialogos_genera_warning_MANY_DIALOGUES() {
        WorldSnapshot base = validSnapshot();
        List<DialogueSnap> manyDialogues = new java.util.ArrayList<>();
        for (int i = 0; i < WorldValidationService.DIALOGUE_WARN_THRESHOLD + 1; i++) {
            manyDialogues.add(new DialogueSnap(100L + i, 1L));
        }
        WorldSnapshot snap = new WorldSnapshot(base.caseVersionId(),
                base.nodes(), base.decisions(), base.maps(),
                base.objects(), base.collisions(), manyDialogues, base.hasSafeExit());

        WorldValidationResult result = service.validate(snap);
        // Es WARNING, no ERROR → canPublish sigue siendo true
        assertThat(result.canPublish()).isTrue();
        assertThat(result.warnings()).anySatisfy(i -> assertThat(i.code()).isEqualTo("MANY_DIALOGUES"));
    }
}
