package com.psychosim.domain.reporte;

import com.psychosim.domain.grupo.Grupo;
import com.psychosim.domain.grupo.GrupoRepository;
import com.psychosim.domain.sesion.RespuestaEstudiante;
import com.psychosim.domain.sesion.SesionJuego;
import com.psychosim.domain.sesion.SesionRepository;
import com.psychosim.domain.user.User;
import com.psychosim.simulation.domain.model.AttemptStatus;
import com.psychosim.simulation.domain.model.DecisionClassification;
import com.psychosim.simulation.infrastructure.persistence.AttemptEventJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.ReflectionJournalJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.RubricEvaluationJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SimulationAttemptEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationAttemptJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final SesionRepository sesionRepository;
    private final GrupoRepository grupoRepository;
    private final SimulationAttemptJpaRepository simulationAttemptRepository;
    private final AttemptEventJpaRepository attemptEventRepository;
    private final ReflectionJournalJpaRepository reflectionJournalRepository;
    private final RubricEvaluationJpaRepository rubricEvaluationRepository;

    @Transactional(readOnly = true)
    public ReporteGrupoDTO generarReporteGrupo(Long grupoId, Long casoId, Long caseVersionId) {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new EntityNotFoundException("Grupo no encontrado: " + grupoId));

        ReporteGrupoDTO legacyReport = buildLegacyReport(grupoId, casoId);
        ReporteSimulacionGrupoDTO simulacion = caseVersionId != null
                ? buildSimulationReport(grupo, caseVersionId)
                : null;

        return new ReporteGrupoDTO(
                legacyReport.grupoId(),
                legacyReport.casoId(),
                caseVersionId,
                legacyReport.totalSesiones(),
                legacyReport.puntajePromedio(),
                legacyReport.tasaAciertos(),
                legacyReport.tiempoPromedioMs(),
                legacyReport.estudiantes(),
                simulacion
        );
    }

    private ReporteGrupoDTO buildLegacyReport(Long grupoId, Long casoId) {
        if (casoId == null) {
            return new ReporteGrupoDTO(grupoId, null, null, 0, 0.0, 0.0, 0L, List.of(), null);
        }

        List<SesionJuego> sesiones = sesionRepository.findCompletasByGrupoAndCaso(grupoId, casoId);

        if (sesiones.isEmpty()) {
            return new ReporteGrupoDTO(grupoId, casoId, null, 0, 0.0, 0.0, 0L, List.of(), null);
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

        return new ReporteGrupoDTO(
                grupoId,
                casoId,
                null,
                sesiones.size(),
                puntajePromedio,
                tasaAciertos,
                (long) tiempoPromedio,
                porEstudiante,
                null
        );
    }

    private ReporteSimulacionGrupoDTO buildSimulationReport(Grupo grupo, Long caseVersionId) {
        List<Long> studentIds = grupo.getEstudiantes().stream().map(User::getId).toList();
        if (studentIds.isEmpty()) {
            return emptySimulationReport();
        }

        List<SimulationAttemptEntity> attempts = simulationAttemptRepository
                .findByCaseVersionIdAndStudentIds(caseVersionId, studentIds);
        if (attempts.isEmpty()) {
            return emptySimulationReport();
        }

        int intentosCompletados = (int) attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED)
                .count();
        int intentosEnProgreso = (int) attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                .count();
        int intentosSalidaSegura = (int) attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.SAFE_EXITED)
                .count();

        List<SimulationAttemptEntity> scoredAttempts = attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.SAFE_EXITED)
                .toList();
        double puntajePromedio = scoredAttempts.stream()
                .mapToInt(SimulationAttemptEntity::getAccumulatedScore)
                .average()
                .orElse(0);

        List<UUID> attemptIds = attempts.stream().map(SimulationAttemptEntity::getId).toList();
        long decisionesAdecuadas = sumDecisionCounts(attemptIds, DecisionClassification.ADEQUATE);
        long decisionesRiesgosas = sumDecisionCounts(attemptIds, DecisionClassification.RISKY);
        long decisionesInadecuadas = sumDecisionCounts(attemptIds, DecisionClassification.INADEQUATE);
        int bitacorasRegistradas = (int) reflectionJournalRepository.countByAttemptIdIn(attemptIds);
        int rubricasAplicadas = (int) rubricEvaluationRepository.countDistinctAttemptsWithRubric(attemptIds);

        Map<Long, List<SimulationAttemptEntity>> attemptsByStudent = new LinkedHashMap<>();
        for (SimulationAttemptEntity attempt : attempts) {
            attemptsByStudent.computeIfAbsent(attempt.getStudent().getId(), key -> new ArrayList<>()).add(attempt);
        }

        List<EstudianteSimulacionReporteDTO> estudiantes = grupo.getEstudiantes().stream()
                .map(student -> toEstudianteSimulacionReporte(student, attemptsByStudent.getOrDefault(student.getId(), List.of())))
                .sorted(Comparator.comparing(EstudianteSimulacionReporteDTO::nombre))
                .toList();

        return new ReporteSimulacionGrupoDTO(
                attempts.size(),
                intentosCompletados,
                intentosEnProgreso,
                intentosSalidaSegura,
                puntajePromedio,
                decisionesAdecuadas,
                decisionesRiesgosas,
                decisionesInadecuadas,
                bitacorasRegistradas,
                rubricasAplicadas,
                estudiantes
        );
    }

    private EstudianteSimulacionReporteDTO toEstudianteSimulacionReporte(User student, List<SimulationAttemptEntity> attempts) {
        int completados = (int) attempts.stream().filter(a -> a.getStatus() == AttemptStatus.COMPLETED).count();
        int enProgreso = (int) attempts.stream().filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS).count();
        int salidaSegura = (int) attempts.stream().filter(a -> a.getStatus() == AttemptStatus.SAFE_EXITED).count();
        double puntajePromedio = attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.SAFE_EXITED)
                .mapToInt(SimulationAttemptEntity::getAccumulatedScore)
                .average()
                .orElse(0);

        List<UUID> attemptIds = attempts.stream().map(SimulationAttemptEntity::getId).toList();
        long adecuadas = sumDecisionCounts(attemptIds, DecisionClassification.ADEQUATE);
        long riesgosas = sumDecisionCounts(attemptIds, DecisionClassification.RISKY);
        long inadecuadas = sumDecisionCounts(attemptIds, DecisionClassification.INADEQUATE);
        int bitacoras = attemptIds.isEmpty() ? 0 : (int) reflectionJournalRepository.countByAttemptIdIn(attemptIds);
        int rubricas = attemptIds.isEmpty() ? 0 : (int) rubricEvaluationRepository.countDistinctAttemptsWithRubric(attemptIds);

        String estado = enProgreso > 0 ? "EN_PROGRESO"
                : completados > 0 ? "COMPLETADO"
                : salidaSegura > 0 ? "SAFE_EXITED"
                : "PENDIENTE";

        return new EstudianteSimulacionReporteDTO(
                student.getId(),
                student.getNombreCompleto(),
                attempts.size(),
                completados,
                enProgreso,
                salidaSegura,
                puntajePromedio,
                adecuadas,
                riesgosas,
                inadecuadas,
                bitacoras,
                rubricas,
                estado
        );
    }

    private long sumDecisionCounts(List<UUID> attemptIds, DecisionClassification classification) {
        return attemptIds.stream()
                .mapToLong(id -> attemptEventRepository.countDecisionsByClassification(id, classification))
                .sum();
    }

    private ReporteSimulacionGrupoDTO emptySimulationReport() {
        return new ReporteSimulacionGrupoDTO(0, 0, 0, 0, 0.0, 0, 0, 0, 0, 0, List.of());
    }

    @Transactional(readOnly = true)
    public ReporteGrupoDTO generarReporteGrupo(Long grupoId, Long casoId) {
        return generarReporteGrupo(grupoId, casoId, null);
    }

    @Transactional(readOnly = true)
    public String exportarCsv(Long grupoId, Long casoId, Long caseVersionId) {
        ReporteGrupoDTO reporte = generarReporteGrupo(grupoId, casoId, caseVersionId);
        var sb = new StringBuilder();
        if (reporte.simulacion() != null) {
            sb.append("Estudiante,Intentos,Completados,En progreso,Salida segura,Puntaje prom.,Adecuadas,Riesgosas,Inadecuadas,Bitacoras,Rubricas,Estado\n");
            for (var e : reporte.simulacion().estudiantes()) {
                sb.append(String.format("%s,%d,%d,%d,%d,%.1f,%d,%d,%d,%d,%d,%s\n",
                        e.nombre(),
                        e.totalIntentos(),
                        e.intentosCompletados(),
                        e.intentosEnProgreso(),
                        e.intentosSalidaSegura(),
                        e.puntajePromedio(),
                        e.decisionesAdecuadas(),
                        e.decisionesRiesgosas(),
                        e.decisionesInadecuadas(),
                        e.bitacorasRegistradas(),
                        e.rubricasAplicadas(),
                        e.estado()));
            }
            return sb.toString();
        }

        sb.append("Estudiante,Puntaje,% Aciertos,Tiempo Promedio (ms),Estado\n");
        for (var e : reporte.estudiantes()) {
            sb.append(String.format("%s,%d,%.1f,%.0f,%s\n",
                    e.nombre(), e.puntaje(), e.porcentajeAciertos(), e.tiempoPromedioMs(), e.estado()));
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportarCsv(Long grupoId, Long casoId) {
        return exportarCsv(grupoId, casoId, null);
    }

    @Transactional(readOnly = true)
    public DashboardDTO getDashboard() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        long sesionesCompletadasHoy = sesionRepository.countCompletadasHoy();
        long simulacionesCompletadasHoy = simulationAttemptRepository.countCompletedSince(startOfDay);
        long simulacionesEnProgreso = simulationAttemptRepository.countByStatusIn(List.of(AttemptStatus.IN_PROGRESS));
        long simulacionesCompletadas = simulationAttemptRepository.countByStatusIn(
                List.of(AttemptStatus.COMPLETED, AttemptStatus.SAFE_EXITED));

        double puntajePromedioLegacy = sesionRepository.avgPuntajeGlobal() != null
                ? sesionRepository.avgPuntajeGlobal() : 0;
        double puntajePromedioSimulacion = simulationAttemptRepository.averageCompletedScore() != null
                ? simulationAttemptRepository.averageCompletedScore() : 0;

        var ultimasSesiones = sesionRepository.findUltimasDiez().stream()
                .map(s -> new SesionResumenDTO(
                        s.getId(), s.getCaso().getTitulo(),
                        s.getEstudiante().getNombreCompleto(),
                        s.getPuntajeTotal(), s.isCompletado()
                )).toList();

        var ultimosIntentos = simulationAttemptRepository.findTop10ByOrderByStartedAtDesc().stream()
                .map(this::toSimulacionResumen)
                .toList();

        var intentosRecientes = Stream.concat(
                        ultimasSesiones.stream().map(s -> new IntentoRecienteDTO(
                                "legacy-" + s.id(),
                                s.casoTitulo(),
                                s.estudiante(),
                                s.puntaje(),
                                s.completado() ? "COMPLETADO" : "EN_PROGRESO",
                                "LEGACY"
                        )),
                        ultimosIntentos.stream().map(s -> new IntentoRecienteDTO(
                                s.id(),
                                s.casoTitulo(),
                                s.estudiante(),
                                s.puntaje(),
                                s.estado(),
                                "SIMULATION"
                        ))
                )
                .sorted(Comparator.comparing(IntentoRecienteDTO::puntaje).reversed())
                .limit(10)
                .toList();

        return new DashboardDTO(
                sesionesCompletadasHoy + simulacionesCompletadasHoy,
                simulacionesCompletadasHoy,
                puntajePromedioSimulacion > 0 ? puntajePromedioSimulacion : puntajePromedioLegacy,
                ultimasSesiones,
                simulacionesCompletadas,
                simulacionesEnProgreso,
                puntajePromedioSimulacion,
                attemptEventRepository.countAllDecisionsByClassification(DecisionClassification.ADEQUATE),
                attemptEventRepository.countAllDecisionsByClassification(DecisionClassification.RISKY),
                attemptEventRepository.countAllDecisionsByClassification(DecisionClassification.INADEQUATE),
                attemptEventRepository.countProhibitedDecisions(),
                ultimosIntentos,
                intentosRecientes
        );
    }

    private SimulacionResumenDTO toSimulacionResumen(SimulationAttemptEntity attempt) {
        return new SimulacionResumenDTO(
                attempt.getId().toString(),
                attempt.getCaseVersion().getSimulationCase().getTitle(),
                attempt.getStudent().getNombreCompleto(),
                attempt.getAccumulatedScore(),
                attempt.getStatus().name()
        );
    }

    public record ReporteGrupoDTO(
            Long grupoId,
            Long casoId,
            Long caseVersionId,
            int totalSesiones,
            double puntajePromedio,
            double tasaAciertos,
            long tiempoPromedioMs,
            List<EstudianteReporteDTO> estudiantes,
            ReporteSimulacionGrupoDTO simulacion
    ) {}

    public record ReporteSimulacionGrupoDTO(
            int totalIntentos,
            int intentosCompletados,
            int intentosEnProgreso,
            int intentosSalidaSegura,
            double puntajePromedio,
            long decisionesAdecuadas,
            long decisionesRiesgosas,
            long decisionesInadecuadas,
            int bitacorasRegistradas,
            int rubricasAplicadas,
            List<EstudianteSimulacionReporteDTO> estudiantes
    ) {}

    public record EstudianteSimulacionReporteDTO(
            Long id,
            String nombre,
            int totalIntentos,
            int intentosCompletados,
            int intentosEnProgreso,
            int intentosSalidaSegura,
            double puntajePromedio,
            long decisionesAdecuadas,
            long decisionesRiesgosas,
            long decisionesInadecuadas,
            int bitacorasRegistradas,
            int rubricasAplicadas,
            String estado
    ) {}

    public record EstudianteReporteDTO(Long id, String nombre, int puntaje,
                                       double porcentajeAciertos, double tiempoPromedioMs,
                                       String estado) {}

    public record DashboardDTO(
            long estudiantesActivos,
            long casosCompletadosHoy,
            double puntajePromedioGlobal,
            List<SesionResumenDTO> ultimasSesiones,
            long simulacionesCompletadas,
            long simulacionesEnProgreso,
            double puntajePromedioSimulacion,
            long decisionesAdecuadas,
            long decisionesRiesgosas,
            long decisionesInadecuadas,
            long decisionesProhibidas,
            List<SimulacionResumenDTO> ultimosIntentos,
            List<IntentoRecienteDTO> intentosRecientes
    ) {}

    public record SesionResumenDTO(Long id, String casoTitulo, String estudiante,
                                   int puntaje, boolean completado) {}

    public record SimulacionResumenDTO(String id, String casoTitulo, String estudiante,
                                       int puntaje, String estado) {}

    public record IntentoRecienteDTO(String id, String casoTitulo, String estudiante,
                                    int puntaje, String estado, String origen) {}
}
