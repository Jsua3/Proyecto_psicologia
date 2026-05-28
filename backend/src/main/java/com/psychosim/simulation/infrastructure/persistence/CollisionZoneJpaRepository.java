package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollisionZoneJpaRepository extends JpaRepository<CollisionZoneEntity, Long> {
    List<CollisionZoneEntity> findBySceneMapIdOrderById(Long sceneMapId);
}
