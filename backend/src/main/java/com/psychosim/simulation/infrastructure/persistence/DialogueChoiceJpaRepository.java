package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DialogueChoiceJpaRepository extends JpaRepository<DialogueChoiceEntity, Long> {
    List<DialogueChoiceEntity> findByDialogueTreeIdOrderByDisplayOrder(Long dialogueTreeId);
}
