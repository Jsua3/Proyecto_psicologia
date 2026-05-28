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
@Table(name = "dialogue_lines")
@Getter
@Setter
@NoArgsConstructor
public class DialogueLineEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dialogue_tree_id", nullable = false)
    private DialogueTreeEntity dialogueTree;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;

    @Column(name = "speaker_name", nullable = false, length = 160)
    private String speakerName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false, length = 60)
    private String emotion = "neutral";
}
