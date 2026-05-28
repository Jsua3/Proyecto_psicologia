package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DialogueTreeJpaRepository extends JpaRepository<DialogueTreeEntity, Long> {
    Optional<DialogueTreeEntity> findByMapObjectId(Long mapObjectId);

    List<DialogueTreeEntity> findBySceneMapIdOrderById(Long sceneMapId);
}
