package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DialogueLineJpaRepository extends JpaRepository<DialogueLineEntity, Long> {
    List<DialogueLineEntity> findByDialogueTreeIdOrderByDisplayOrder(Long dialogueTreeId);
    void deleteAllByDialogueTreeId(Long dialogueTreeId);
}
