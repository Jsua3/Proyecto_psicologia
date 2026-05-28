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
@Table(name = "scene_maps")
@Getter
@Setter
@NoArgsConstructor
public class SceneMapEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_version_id", nullable = false)
    private CaseVersionEntity caseVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private SimulationNodeEntity node;

    @Column(name = "map_key", nullable = false, length = 120)
    private String mapKey;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(nullable = false)
    private int width = 960;

    @Column(nullable = false)
    private int height = 540;

    @Column(nullable = false, length = 80)
    private String theme = "clinical-soft";

    @Column(name = "spawn_x", nullable = false)
    private int spawnX = 145;

    @Column(name = "spawn_y", nullable = false)
    private int spawnY = 430;

    @Column(name = "ambient_json", nullable = false, columnDefinition = "TEXT")
    private String ambientJson = "{}";
}
