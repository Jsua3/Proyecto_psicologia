package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MapObjectJpaRepository extends JpaRepository<MapObjectEntity, Long> {
    List<MapObjectEntity> findBySceneMapIdAndVisibleTrueOrderById(Long sceneMapId);

    List<MapObjectEntity> findBySceneMapIdOrderById(Long sceneMapId);

    Optional<MapObjectEntity> findBySceneMapIdAndObjectKey(Long sceneMapId, String objectKey);
}
