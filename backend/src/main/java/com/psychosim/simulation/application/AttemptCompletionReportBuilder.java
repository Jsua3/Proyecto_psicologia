package com.psychosim.simulation.application;

import com.psychosim.simulation.domain.model.AttemptEventType;
import com.psychosim.simulation.domain.model.AttemptStatus;
import com.psychosim.simulation.infrastructure.persistence.AttemptEventEntity;
import com.psychosim.simulation.infrastructure.persistence.ReflectionJournalJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SimulationAttemptEntity;
import com.psychosim.simulation.web.SimulationDtos.AttemptCompletionReport;
import com.psychosim.simulation.web.SimulationDtos.SimulationMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AttemptCompletionReportBuilder {

    private final ReflectionJournalJpaRepository reflectionRepository;

    public AttemptCompletionReport build(SimulationAttemptEntity attempt, List<AttemptEventEntity> events) {
        int adequate = 0;
        int risky = 0;
        int inadequate = 0;
        int prohibited = 0;
        int toolsUsed = 0;
        Set<String> visitedNodes = new LinkedHashSet<>();
        List<String> competencies = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        for (AttemptEventEntity event : events) {
            if (event.getNode() != null && event.getEventType() == AttemptEventType.NODE_ENTERED) {
                visitedNodes.add(event.getNode().getTitle());
            }
            if (event.getEventType() == AttemptEventType.TOOL_USED) {
                toolsUsed++;
            }
            if (event.getDecisionOption() == null) {
                continue;
            }
            switch (event.getDecisionOption().getClassification()) {
                case ADEQUATE -> adequate++;
                case RISKY -> risky++;
                case INADEQUATE -> inadequate++;
            }
            if (event.getDecisionOption().isProhibitedConduct()) {
                prohibited++;
            }
        }

        if (attempt.isInstitutionalRouteActivated()) {
            competencies.add("Articulación con rutas institucionales de protección");
        }
        if (adequate > 0) {
            competencies.add("Comunicación ética y contención emocional");
        }
        if (toolsUsed > 0) {
            competencies.add("Uso pertinente de herramientas clínicas");
        }
        if (reflectionRepository.countByAttemptId(attempt.getId()) > 0) {
            competencies.add("Reflexión clínica documentada");
        }

        if (risky > 0 || inadequate > 0) {
            recommendations.add("Revisa las decisiones riesgosas o inadecuadas con tu docente en sesión de retroalimentación.");
        }
        if (attempt.getStressIndex() >= 70) {
            recommendations.add("Practica estrategias de autorregulación antes de escenarios de alta carga emocional.");
        }
        if (attempt.isRevictimizationRisk()) {
            recommendations.add("Repasa los principios de no revictimización y el marco ético de intervención.");
        }
        if (attempt.getStatus() == AttemptStatus.SAFE_EXITED) {
            recommendations.add("Retoma el caso con acompañamiento docente cuando te sientas preparado/a.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Consolida el aprendizaje repitiendo el caso con foco en la ruta institucional y la bitácora reflexiva.");
        }

        String summary = switch (attempt.getStatus()) {
            case COMPLETED -> "Intento finalizado. Tu recorrido quedó registrado para evaluación formativa.";
            case SAFE_EXITED -> "Salida segura registrada. No se aplicó penalización; el intento quedó disponible para revisión docente.";
            case IN_PROGRESS -> "Intento en progreso.";
        };

        return new AttemptCompletionReport(
                attempt.getId(),
                attempt.getCaseVersion().getSimulationCase().getTitle(),
                attempt.getStatus().name(),
                attempt.getAccumulatedScore(),
                attempt.getStressIndex(),
                toMetrics(attempt),
                adequate,
                risky,
                inadequate,
                prohibited,
                toolsUsed,
                (int) reflectionRepository.countByAttemptId(attempt.getId()),
                attempt.getStatus() == AttemptStatus.SAFE_EXITED,
                List.copyOf(visitedNodes),
                List.copyOf(competencies),
                List.copyOf(recommendations),
                summary
        );
    }

    public SimulationMetrics toMetrics(SimulationAttemptEntity attempt) {
        return new SimulationMetrics(
                attempt.getAccumulatedScore(),
                attempt.getStressIndex(),
                attempt.getVictimRisk(),
                attempt.getUserTrust(),
                attempt.isInstitutionalRouteActivated(),
                attempt.isRevictimizationRisk()
        );
    }
}
