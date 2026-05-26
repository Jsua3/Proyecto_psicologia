package com.psychosim.domain.sesion;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SesionRepository extends JpaRepository<SesionJuego, Long> {

    List<SesionJuego> findByEstudianteIdOrderByFechaInicioDesc(Long estudianteId);

    List<SesionJuego> findByCasoIdAndCompletadoTrue(Long casoId);

    // Conteo portátil: se llama pasando inicio/fin del día desde el servicio
    @Query("SELECT COUNT(s) FROM SesionJuego s WHERE s.completado = true AND s.fechaFin >= :inicio AND s.fechaFin < :fin")
    long countCompletadasEntre(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    default long countCompletadasHoy() {
        LocalDateTime hoy = LocalDateTime.now().toLocalDate().atStartOfDay();
        return countCompletadasEntre(hoy, hoy.plusDays(1));
    }

    @Query("SELECT AVG(s.puntajeTotal) FROM SesionJuego s WHERE s.completado = true")
    Double avgPuntajeGlobal();

    @Query("""
        SELECT s FROM SesionJuego s
        WHERE s.estudiante.id IN (
            SELECT e.id FROM com.psychosim.domain.grupo.Grupo g JOIN g.estudiantes e WHERE g.id = :grupoId
        )
        AND s.caso.id = :casoId
        AND s.completado = true
        """)
    List<SesionJuego> findCompletasByGrupoAndCaso(@Param("grupoId") Long grupoId, @Param("casoId") Long casoId);

    @Query("SELECT s FROM SesionJuego s WHERE s.completado = true ORDER BY s.fechaFin DESC")
    List<SesionJuego> findCompletadasOrdenadas(Pageable pageable);

    default List<SesionJuego> findUltimasDiez() {
        return findCompletadasOrdenadas(PageRequest.of(0, 10));
    }
}
