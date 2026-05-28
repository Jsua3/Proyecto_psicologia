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
@Table(name = "clinical_tools")
@Getter
@Setter
@NoArgsConstructor
public class ClinicalToolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_version_id")
    private CaseVersionEntity caseVersion;

    @Column(name = "tool_code", nullable = false, length = 80)
    private String toolCode;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false, length = 80)
    private String icon = "psychology";

    @Column(nullable = false, length = 80)
    private String category = "clinical";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
