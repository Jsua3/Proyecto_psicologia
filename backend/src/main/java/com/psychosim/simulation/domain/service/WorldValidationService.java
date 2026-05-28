package com.psychosim.simulation.domain.service;

import com.psychosim.simulation.domain.model.WorldSnapshot;
import com.psychosim.simulation.domain.model.WorldSnapshot.CollisionSnap;
import com.psychosim.simulation.domain.model.WorldSnapshot.DecisionSnap;
import com.psychosim.simulation.domain.model.WorldSnapshot.MapSnap;
import com.psychosim.simulation.domain.model.WorldSnapshot.NodeSnap;
import com.psychosim.simulation.domain.model.WorldSnapshot.ObjectSnap;
import com.psychosim.simulation.domain.service.WorldValidationResult.Severity;
import com.psychosim.simulation.domain.service.WorldValidationResult.WorldValidationIssue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servicio de dominio puro: valida el mundo de un caso de simulación.
 * Sin dependencias de Spring, JPA ni frameworks externos.
 * Puede instanciarse directamente en pruebas unitarias.
 */
public class WorldValidationService {

    // ─── Límites de publicación (Decisión #4) ────────────────────────────────────
    public static final int MAX_OBJECTS_PER_MAP     = 250;
    public static final int MAX_COLLISIONS_PER_MAP  = 300;
    public static final int MAX_TRIGGERS_PER_MAP    = 100;
    public static final int MAX_MAP_WIDTH           = 2560;
    public static final int MAX_MAP_HEIGHT          = 1920;
    public static final int DIALOGUE_WARN_THRESHOLD = 50;

    public WorldValidationResult validate(WorldSnapshot snapshot) {
        List<WorldValidationIssue> issues = new ArrayList<>();

        validateGraph(snapshot.nodes(), snapshot.decisions(), issues);
        validateGeometry(snapshot, issues);
        validateEthics(snapshot.decisions(), issues);
        validateSafeExit(snapshot.hasSafeExit(), issues);
        validateLimits(snapshot, issues);

        return new WorldValidationResult(List.copyOf(issues));
    }

    // ─── DAG ─────────────────────────────────────────────────────────────────────

    private void validateGraph(List<NodeSnap> nodes, List<DecisionSnap> decisions,
                               List<WorldValidationIssue> issues) {
        if (nodes.isEmpty()) {
            issues.add(err("NO_NODES", "El caso debe tener al menos un nodo", null));
            return;
        }

        long startCount = nodes.stream().filter(NodeSnap::startNode).count();
        if (startCount == 0) {
            issues.add(err("NO_START_NODE",
                    "El grafo debe tener exactamente un nodo inicial; no se encontró ninguno", null));
        } else if (startCount > 1) {
            issues.add(err("MULTIPLE_START_NODES",
                    "El grafo tiene " + startCount + " nodos iniciales; debe tener exactamente 1", null));
        }

        long terminalCount = nodes.stream().filter(NodeSnap::terminalNode).count();
        if (terminalCount == 0) {
            issues.add(err("NO_TERMINAL_NODE",
                    "El grafo debe tener al menos un nodo terminal (fin de simulación)", null));
        }

        detectCycle(nodes, decisions, issues);
    }

    private void detectCycle(List<NodeSnap> nodes, List<DecisionSnap> decisions,
                              List<WorldValidationIssue> issues) {
        Map<Long, List<Long>> adjacency = new HashMap<>();
        for (NodeSnap n : nodes) adjacency.put(n.id(), new ArrayList<>());
        for (DecisionSnap d : decisions)
            adjacency.computeIfAbsent(d.sourceNodeId(), k -> new ArrayList<>()).add(d.targetNodeId());

        Set<Long> visiting = new HashSet<>();
        Set<Long> visited  = new HashSet<>();

        for (NodeSnap n : nodes) {
            if (!visited.contains(n.id())) {
                if (hasCycleDfs(n.id(), adjacency, visiting, visited)) {
                    issues.add(err("GRAPH_CYCLE",
                            "El grafo contiene un ciclo; los ciclos impiden que la simulación pueda finalizar", null));
                    return; // un reporte es suficiente
                }
            }
        }
    }

    private boolean hasCycleDfs(long id, Map<Long, List<Long>> adj,
                                 Set<Long> visiting, Set<Long> visited) {
        if (!visiting.add(id)) return true;
        for (long next : adj.getOrDefault(id, List.of())) {
            if (!visited.contains(next) && hasCycleDfs(next, adj, visiting, visited)) return true;
        }
        visiting.remove(id);
        visited.add(id);
        return false;
    }

    // ─── Geometría ────────────────────────────────────────────────────────────────

    private void validateGeometry(WorldSnapshot snapshot, List<WorldValidationIssue> issues) {
        Map<Long, MapSnap> byId = new HashMap<>();
        for (MapSnap map : snapshot.maps()) {
            byId.put(map.id(), map);
            if (map.spawnX() < 0 || map.spawnX() > map.width() ||
                    map.spawnY() < 0 || map.spawnY() > map.height()) {
                issues.add(err("SPAWN_OUT_OF_BOUNDS",
                        "El spawn (" + map.spawnX() + "," + map.spawnY() +
                                ") está fuera del mapa id=" + map.id(),
                        "map:" + map.id()));
            }
        }

        for (ObjectSnap obj : snapshot.objects()) {
            MapSnap map = byId.get(obj.mapId());
            if (map != null && !inBounds(obj.positionX(), obj.positionY(), obj.width(), obj.height(), map)) {
                issues.add(err("OBJECT_OUT_OF_BOUNDS",
                        "Objeto id=" + obj.id() + " está fuera del mapa id=" + obj.mapId(),
                        "object:" + obj.id()));
            }
        }

        for (CollisionSnap col : snapshot.collisions()) {
            MapSnap map = byId.get(col.mapId());
            if (map != null && !inBounds(col.positionX(), col.positionY(), col.width(), col.height(), map)) {
                issues.add(err("COLLISION_OUT_OF_BOUNDS",
                        "Colisión id=" + col.id() + " está fuera del mapa id=" + col.mapId(),
                        "collision:" + col.id()));
            }
        }
    }

    private boolean inBounds(int x, int y, int w, int h, MapSnap map) {
        return x >= 0 && y >= 0 && (x + w) <= map.width() && (y + h) <= map.height();
    }

    // ─── Ética ───────────────────────────────────────────────────────────────────

    private void validateEthics(List<DecisionSnap> decisions, List<WorldValidationIssue> issues) {
        for (DecisionSnap d : decisions) {
            if (d.prohibitedConduct() &&
                    (d.prohibitionReason() == null || d.prohibitionReason().isBlank())) {
                issues.add(err("PROHIBITED_WITHOUT_REASON",
                        "La decisión prohibida id=" + d.id() +
                                " debe documentar la razón de prohibición",
                        "decision:" + d.id()));
            }
        }
    }

    // ─── Salida segura ────────────────────────────────────────────────────────────

    private void validateSafeExit(boolean hasSafeExit, List<WorldValidationIssue> issues) {
        if (!hasSafeExit) {
            issues.add(err("NO_SAFE_EXIT",
                    "El caso debe tener al menos un objeto EXIT configurado como salida segura", null));
        }
    }

    // ─── Límites ─────────────────────────────────────────────────────────────────

    private void validateLimits(WorldSnapshot snapshot, List<WorldValidationIssue> issues) {
        for (MapSnap map : snapshot.maps()) {
            // Tamaño del mapa
            if (map.width() > MAX_MAP_WIDTH || map.height() > MAX_MAP_HEIGHT) {
                issues.add(err("MAP_TOO_LARGE",
                        "Mapa id=" + map.id() + " (" + map.width() + "×" + map.height() + "px)" +
                                " excede el límite " + MAX_MAP_WIDTH + "×" + MAX_MAP_HEIGHT + "px",
                        "map:" + map.id()));
            }

            // Objetos
            long objCount = snapshot.objects().stream().filter(o -> o.mapId() == map.id()).count();
            if (objCount > MAX_OBJECTS_PER_MAP) {
                issues.add(err("TOO_MANY_OBJECTS",
                        "Mapa id=" + map.id() + " tiene " + objCount + " objetos (máx " + MAX_OBJECTS_PER_MAP + ")",
                        "map:" + map.id()));
            }

            // Colisiones
            long colCount = snapshot.collisions().stream().filter(c -> c.mapId() == map.id()).count();
            if (colCount > MAX_COLLISIONS_PER_MAP) {
                issues.add(err("TOO_MANY_COLLISIONS",
                        "Mapa id=" + map.id() + " tiene " + colCount + " colisiones (máx " + MAX_COLLISIONS_PER_MAP + ")",
                        "map:" + map.id()));
            }

            // Triggers
            long trigCount = snapshot.objects().stream()
                    .filter(o -> o.mapId() == map.id() && "TRIGGER".equalsIgnoreCase(o.objectType()))
                    .count();
            if (trigCount > MAX_TRIGGERS_PER_MAP) {
                issues.add(err("TOO_MANY_TRIGGERS",
                        "Mapa id=" + map.id() + " tiene " + trigCount + " triggers (máx " + MAX_TRIGGERS_PER_MAP + ")",
                        "map:" + map.id()));
            }

            // Diálogos — warning (no error)
            long dlgCount = snapshot.dialogues().stream().filter(d -> d.mapId() == map.id()).count();
            if (dlgCount >= DIALOGUE_WARN_THRESHOLD) {
                issues.add(warn("MANY_DIALOGUES",
                        "Mapa id=" + map.id() + " tiene " + dlgCount + " diálogos; considera distribuirlos en más nodos",
                        "map:" + map.id()));
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private WorldValidationIssue err(String code, String message, String ref) {
        return new WorldValidationIssue(Severity.ERROR, code, message, ref);
    }

    private WorldValidationIssue warn(String code, String message, String ref) {
        return new WorldValidationIssue(Severity.WARNING, code, message, ref);
    }
}
