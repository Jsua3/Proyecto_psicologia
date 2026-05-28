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
@Table(name = "dialogue_choices")
@Getter
@Setter
@NoArgsConstructor
public class DialogueChoiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dialogue_tree_id", nullable = false)
    private DialogueTreeEntity dialogueTree;

    @Column(name = "choice_key", nullable = false, length = 120)
    private String choiceKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_option_id")
    private DecisionOptionEntity decisionOption;

    @Column(name = "required_tool_code", length = 80)
    private String requiredToolCode;

    @Column(name = "effect_json", nullable = false, columnDefinition = "TEXT")
    private String effectJson = "{}";

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
}
