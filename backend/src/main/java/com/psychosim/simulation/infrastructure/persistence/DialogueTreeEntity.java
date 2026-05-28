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
@Table(name = "dialogue_trees")
@Getter
@Setter
@NoArgsConstructor
public class DialogueTreeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_map_id", nullable = false)
    private SceneMapEntity sceneMap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_object_id")
    private MapObjectEntity mapObject;

    @Column(name = "tree_key", nullable = false, length = 120)
    private String treeKey;

    @Column(name = "speaker_name", nullable = false, length = 160)
    private String speakerName;

    @Column(name = "portrait_key", length = 120)
    private String portraitKey;

    @Column(nullable = false, length = 60)
    private String emotion = "neutral";
}
