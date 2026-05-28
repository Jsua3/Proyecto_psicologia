package com.psychosim.simulation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attempt_world_states")
@Getter
@Setter
@NoArgsConstructor
public class AttemptWorldStateEntity {
    @Id
    @Column(name = "attempt_id")
    private UUID attemptId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "attempt_id")
    private SimulationAttemptEntity attempt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_map_id")
    private SceneMapEntity sceneMap;

    @Column(name = "player_x", nullable = false)
    private int playerX = 145;

    @Column(name = "player_y", nullable = false)
    private int playerY = 430;

    @Column(name = "inventory_json", nullable = false, columnDefinition = "TEXT")
    private String inventoryJson = "[]";

    @Column(name = "inspected_object_keys_json", nullable = false, columnDefinition = "TEXT")
    private String inspectedObjectKeysJson = "[]";

    @Column(name = "viewed_dialogue_keys_json", nullable = false, columnDefinition = "TEXT")
    private String viewedDialogueKeysJson = "[]";

    @Column(name = "used_tool_keys_json", nullable = false, columnDefinition = "TEXT")
    private String usedToolKeysJson = "[]";

    @Column(name = "flags_json", nullable = false, columnDefinition = "TEXT")
    private String flagsJson = "{}";

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
