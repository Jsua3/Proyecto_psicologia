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
@Table(name = "simulation_nodes")
@Getter
@Setter
@NoArgsConstructor
public class SimulationNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_version_id", nullable = false)
    private CaseVersionEntity caseVersion;

    @Column(name = "node_key", nullable = false, length = 120)
    private String nodeKey;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String narrative;

    @Column(name = "support_resources_json", nullable = false, columnDefinition = "TEXT")
    private String supportResourcesJson = "[]";

    @Column(name = "required_tools_json", nullable = false, columnDefinition = "TEXT")
    private String requiredToolsJson = "[]";

    @Column(name = "sensitive_content", nullable = false)
    private boolean sensitiveContent;

    @Column(name = "safe_exit_required", nullable = false)
    private boolean safeExitRequired;

    @Column(name = "warning_message", columnDefinition = "TEXT")
    private String warningMessage;

    @Column(name = "start_node", nullable = false)
    private boolean startNode;

    @Column(name = "terminal_node", nullable = false)
    private boolean terminalNode;

    @Column(name = "position_x")
    private Integer positionX;

    @Column(name = "position_y")
    private Integer positionY;
}
