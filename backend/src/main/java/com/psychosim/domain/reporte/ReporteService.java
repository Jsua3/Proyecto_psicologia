package com.psychosim.domain.reporte;

import com.psychosim.domain.grupo.GrupoRepository;
import com.psychosim.domain.sesion.RespuestaEstudiante;
import com.psychosim.domain.sesion.SesionJuego;
import com.psychosim.domain.sesion.SesionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final SesionRepository sesionRepository;
    private final GrupoRepository grupoRepository;

    @Transactional(readOnly = true)
    public ReporteGrupoDTO generarReporteGrupo(Long grupoId, Long casoId) {
        grupoRepository.findById(grupoId)
                .orElseThrow(() -> new EntityNotFoundException("Grupo no encontrado: " + grupoId));

        List<SesionJuego> sesiones = sesionRepository.findCompletasByGrupoAndCaso(grupoId, casoId);

        if (sesiones.isEmpty()) {
            return new ReporteGrupoDTO(grupoId, casoId, 0, 0.0, 0.0, 0L, List.of());
        }

        double puntajePromedio = sesiones.stream()
                .mapToInt(SesionJuego::getPuntajeTotal)
                .average().orElse(0);

        long totalRespuestas = sesiones.stream()
                .mapToLong(s -> s.getRespuestas().size())
                .sum();
        long correctas = sesiones.stream()
                .flatMap(s -> s.getRespuestas().stream())
                .filter(RespuestaEstudiante::isEsCorrecta)
                .count();
        double tasaAciertos = totalRespuestas > 0 ? (double) correctas / totalRespuestas * 100 : 0;

        double tiempoPromedio = sesiones.stream()
                .flatMap(s -> s.getRespuestas().stream())
                .filter(r -> r.getTiempoRespuestaMs() != null)
                .mapToInt(RespuestaEstudiante::getTiempoRespuestaMs)
                .average().orElse(0);

        List<EstudianteReporteDTO> porEstudiante = sesiones.stream()
                .map(s -> {
                    long total = s.getRespuestas().size();
                    long corr = s.getRespuestas().stream().filter(RespuestaEstudiante::isEsCorrecta).count();
                    double tiempo = s.getRespuestas().stream()
                            .filter(r -> r.getTiempoRespuestaMs() != null)
                            .mapToInt(RespuestaEstudiante::getTiempoRespuestaMs)
                            .average().orElse(0);
                    return new EstudianteReporteDTO(
                            s.getEstudiante().getId(),
                            s.getEstudiante().getNombreCompleto(),
                            s.getPuntajeTotal(),
                            total > 0 ? (double) corr / total * 100 : 0,
                            tiempo,
                            s.isCompletado() ? "COMPLETADO" : "EN_PROGRESO"
                    );
                }).toList();

        return new ReporteGrupoDTO(grupoId, casoId, sesiones.size(),
                puntajePromedio, tasaAciertos, (long) tiempoPromedio, porEstudiante);
    }

    @Transactional(readOnly = true)
    public String exportarCsv(Long grupoId, Long casoId) {
        ReporteGrupoDTO reporte = generarReporteGrupo(grupoId, casoId);
        var sb = new StringBuilder();
        sb.append("Estudiante,Puntaje,% Aciertos,Tiempo Promedio (ms),Estado\n");
        for (var e : reporte.estudiantes()) {
            sb.append(String.format("%s,%d,%.1f,%.0f,%s\n",
                    e.nombre(), e.puntaje(), e.porcentajeAciertos(), e.tiempoPromedioMs(), e.estado()));
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public DashboardDTO getDashboard() {
        long estudiantesActivos = sesionRepository.countCompletadasHoy();
        long completadasHoy = sesionRepository.countCompletadasHoy();
        double puntajePromedio = sesionRepository.avgPuntajeGlobal() != null
                ? sesionRepository.avgPuntajeGlobal() : 0;
        var ultimas = sesionRepository.findUltimasDiez().stream()
                .map(s -> new SesionResumenDTO(
                        s.getId(), s.getCaso().getTitulo(),
                        s.getEstudiante().getNombreCompleto(),
                        s.getPuntajeTotal(), s.isCompletado()
                )).toList();
        return new DashboardDTO(estudiantesActivos, completadasHoy, puntajePromedio, ultimas);
    }

    public record ReporteGrupoDTO(Long grupoId, Long casoId, int totalSesiones,
                                  double puntajePromedio, double tasaAciertos,
                                  long tiempoPromedioMs, List<EstudianteReporteDTO> estudiantes) {}

    public record EstudianteReporteDTO(Long id, String nombre, int puntaje,
                                       double porcentajeAciertos, double tiempoPromedioMs,
                                       String estado) {}

    public record DashboardDTO(long estudiantesActivos, long casosCompletadosHoy,
                               double puntajePromedioGlobal, List<SesionResumenDTO> ultimasSesiones) {}

    public record SesionResumenDTO(Long id, String casoTitulo, String estudiante,
                                   int puntaje, boolean completado) {}
}
