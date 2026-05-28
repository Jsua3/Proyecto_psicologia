package com.psychosim.simulation.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psychosim.domain.user.User;
import com.psychosim.simulation.infrastructure.persistence.AttemptEventEntity;
import com.psychosim.simulation.infrastructure.persistence.AttemptEventJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.CriterionScoreEntity;
import com.psychosim.simulation.infrastructure.persistence.CriterionScoreJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.ReflectionJournalJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.RubricCriterionEntity;
import com.psychosim.simulation.infrastructure.persistence.RubricCriterionJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.RubricEntity;
import com.psychosim.simulation.infrastructure.persistence.RubricEvaluationEntity;
import com.psychosim.simulation.infrastructure.persistence.RubricEvaluationJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.RubricJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SimulationAttemptEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationAttemptJpaRepository;
import com.psychosim.simulation.web.SimulationDtos.AttemptTrace;
import com.psychosim.simulation.web.SimulationDtos.CriterionScoreInput;
import com.psychosim.simulation.web.SimulationDtos.CriterionScoreView;
import com.psychosim.simulation.web.SimulationDtos.RecentAttempt;
import com.psychosim.simulation.web.SimulationDtos.ReflectionTrace;
import com.psychosim.simulation.web.SimulationDtos.RubricCriterionView;
import com.psychosim.simulation.web.SimulationDtos.RubricEvaluationView;
import com.psychosim.simulation.web.SimulationDtos.RubricSummary;
import com.psychosim.simulation.web.SimulationDtos.SaveRubricEvaluationRequest;
import com.psychosim.simulation.web.SimulationDtos.TraceEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstructorSimulationService {
    private final SimulationAttemptJpaRepository attemptRepository;
    private final AttemptEventJpaRepository eventRepository;
    private final ReflectionJournalJpaRepository reflectionRepository;
    private final RubricJpaRepository rubricRepository;
    private final RubricCriterionJpaRepository criterionRepository;
    private final RubricEvaluationJpaRepository evaluationRepository;
    private final CriterionScoreJpaRepository scoreRepository;
    private final ReflectionCryptoService reflectionCryptoService;
    private final SimulationWorldService worldService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<RecentAttempt> recentAttempts() {
        return attemptRepository.findTop20ByOrderByStartedAtDesc().stream()
                .map(attempt -> new RecentAttempt(
                        attempt.getId(),
                        anonymize(attempt),
                        attempt.getCaseVersion().getSimulationCase().getTitle(),
                        attempt.getStatus().name(),
                        attempt.getAccumulatedScore(),
                        attempt.getStressIndex(),
                        attempt.getStartedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ))
                .toList();
    }

    @Transactional
    public AttemptTrace trace(UUID attemptId) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId);
        return new AttemptTrace(
                attempt.getId(),
                anonymize(attempt),
                attempt.getCaseVersion().getSimulationCase().getTitle(),
                attempt.getStatus().name(),
                attempt.getAccumulatedScore(),
                attempt.getStressIndex(),
                eventRepository.findByAttemptIdOrderByOccurredAt(attemptId).stream().map(this::toTraceEvent).toList(),
                worldService.worldForAttempt(attempt),
                reflectionRepository.findByAttemptId(attemptId).stream()
                        .map(reflection -> new ReflectionTrace(
                                reflection.getNode().getId(),
                                reflection.getNode().getTitle(),
                                reflectionCryptoService.decrypt(reflection.getEncryptedText()),
                                reflection.isLocked()
                        ))
                        .toList(),
                evaluationRepository.findByAttemptIdOrderByEvaluatedAtDesc(attemptId).stream()
                        .map(evaluation -> new RubricSummary(
                                evaluation.getId(),
                                evaluation.getRubric().getName(),
                                evaluation.getTotalScore().doubleValue(),
                                evaluation.getComment(),
                                evaluation.getEvaluatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public RubricEvaluationView rubric(UUID attemptId) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId);
        RubricEntity rubric = rubricRepository.findFirstByCaseVersionIdAndActiveTrueOrderById(attempt.getCaseVersion().getId())
                .orElseThrow(() -> new EntityNotFoundException("Rubrica no encontrada"));
        List<RubricCriterionEntity> criteria = criterionRepository.findByRubricIdOrderByDisplayOrder(rubric.getId());
        return new RubricEvaluationView(
                rubric.getId(),
                rubric.getName(),
                rubric.getDescription(),
                criteria.stream().map(this::toCriterionView).toList(),
                List.of(),
                null,
                null
        );
    }

    @Transactional
    public RubricEvaluationView saveRubric(UUID attemptId, SaveRubricEvaluationRequest request, User instructor) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId);
        RubricEntity rubric = rubricRepository.findById(request.rubricId())
                .orElseThrow(() -> new EntityNotFoundException("Rubrica no encontrada"));
        RubricEvaluationEntity evaluation = evaluationRepository
                .findByAttemptIdAndRubricIdAndInstructorId(attemptId, rubric.getId(), instructor.getId())
                .orElseGet(RubricEvaluationEntity::new);
        evaluation.setAttempt(attempt);
        evaluation.setRubric(rubric);
        evaluation.setInstructor(instructor);
        evaluation.setComment(request.comment());

        BigDecimal total = request.scores().stream()
                .map(input -> BigDecimal.valueOf(input.score()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        evaluation.setTotalScore(total);
        evaluation = evaluationRepository.save(evaluation);

        scoreRepository.deleteByRubricEvaluationId(evaluation.getId());
        for (CriterionScoreInput input : request.scores()) {
            RubricCriterionEntity criterion = criterionRepository.findById(input.criterionId())
                    .orElseThrow(() -> new EntityNotFoundException("Criterio no encontrado"));
            CriterionScoreEntity score = new CriterionScoreEntity();
            score.setRubricEvaluation(evaluation);
            score.setRubricCriterion(criterion);
            score.setScore(BigDecimal.valueOf(input.score()));
            score.setComment(input.comment());
            score.setEvidenceJson("{}");
            scoreRepository.save(score);
        }

        List<RubricCriterionEntity> criteria = criterionRepository.findByRubricIdOrderByDisplayOrder(rubric.getId());
        List<CriterionScoreEntity> scores = scoreRepository.findByRubricEvaluationId(evaluation.getId());
        return new RubricEvaluationView(
                rubric.getId(),
                rubric.getName(),
                rubric.getDescription(),
                criteria.stream().map(this::toCriterionView).toList(),
                scores.stream().map(this::toScoreView).toList(),
                total.doubleValue(),
                evaluation.getComment()
        );
    }

    private SimulationAttemptEntity requireAttempt(UUID attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("Intento no encontrado"));
    }

    private TraceEvent toTraceEvent(AttemptEventEntity event) {
        return new TraceEvent(
                event.getEventType().name(),
                event.getNode() == null ? null : event.getNode().getTitle(),
                event.getDecisionOption() == null ? null : event.getDecisionOption().getText(),
                event.getScoreDelta(),
                event.getStressDelta(),
                event.getDetail(),
                event.getOccurredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    private RubricCriterionView toCriterionView(RubricCriterionEntity criterion) {
        return new RubricCriterionView(
                criterion.getId(),
                criterion.getCompetency(),
                criterion.getTitle(),
                criterion.getDescription(),
                criterion.getMaxScore(),
                criterion.getDisplayOrder()
        );
    }

    private CriterionScoreView toScoreView(CriterionScoreEntity score) {
        return new CriterionScoreView(
                score.getRubricCriterion().getId(),
                score.getScore().doubleValue(),
                score.getComment(),
                readMap(score.getEvidenceJson())
        );
    }

    private Map<String, Object> readMap(String json) {
        try {
            if (json == null || json.isBlank()) return Map.of();
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String anonymize(SimulationAttemptEntity attempt) {
        String id = attempt.getStudent().getId() == null ? "0" : attempt.getStudent().getId().toString();
        return "Estudiante-" + id;
    }
}
