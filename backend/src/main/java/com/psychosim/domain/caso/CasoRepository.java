package com.psychosim.domain.caso;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CasoRepository extends JpaRepository<Caso, Long> {

    List<Caso> findByActivoTrue();

    Optional<Caso> findByIdAndActivoTrue(Long id);

    // Retorna todos los casos activos — el filtro por grupo se delega al servicio si es necesario
    default List<Caso> findActivosByGrupoId(Long grupoId) {
        return findByActivoTrue();
    }
}
