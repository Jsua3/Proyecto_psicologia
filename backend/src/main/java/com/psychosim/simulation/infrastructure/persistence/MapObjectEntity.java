package com.psychosim.simulation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "map_objects")
@Getter
@Setter
@NoArgsConstructor
public class MapObjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_map_id", nullable = false)
    private SceneMapEntity sceneMap;

    @Column(name = "object_key", nullable = false, length = 120)
    private String objectKey;

    @Column(nullable = false, length = 180)
    private String label;

    @Column(name = "object_type", nullable = false, length = 32)
    private String objectType;

    @Column(name = "position_x", nullable = false)
    private int positionX;

    @Column(name = "position_y", nullable = false)
    private int positionY;

    @Column(nullable = false)
    private int width = 48;

    @Column(nullable = false)
    private int height = 48;

    @Column(name = "color_hex", nullable = false, length = 16)
    private String colorHex = "#4FA3A5";

    @Column(nullable = false, length = 80)
    private String icon = "psychology";

    @Column(name = "short_code", nullable = false, length = 12)
    private String shortCode = "ACT";

    @Column(nullable = false)
    private boolean collision;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(name = "interaction_prompt", nullable = false, length = 180)
    private String interactionPrompt;

    @Column(name = "interaction_text", nullable = false, columnDefinition = "TEXT")
    private String interactionText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_option_id")
    private DecisionOptionEntity decisionOption;

    @Column(name = "tool_code", length = 80)
    private String toolCode;

    @Column(name = "unlock_condition_json", nullable = false, columnDefinition = "TEXT")
    private String unlockConditionJson = "{}";

    // ─── Campos V7 (world_authoring_hardening) ────────────────────────────────────

    /** Capa de renderizado: valores más altos se pintan encima. */
    @Column(name = "z_index", nullable = false)
    private int zIndex = 0;

    /** Dirección de orientación inicial del sprite: down|up|left|right. */
    @Column(nullable = false, length = 16)
    private String facing = "down";

    /** Patrón de movimiento NPC serializado como JSON. */
    @Column(name = "movement_pattern_json", nullable = false, columnDefinition = "TEXT")
    private String movementPatternJson = "{}";

    /** Metadatos adicionales para extensiones futuras. */
    @Column(name = "metadata_json", nullable = false, columnDefinition = "TEXT")
    private String metadataJson = "{}";
}
