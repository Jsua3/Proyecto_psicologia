package com.psychosim.domain.grupo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    List<Grupo> findByProfesorIdAndActivoTrue(Long profesorId);
    Optional<Grupo> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
