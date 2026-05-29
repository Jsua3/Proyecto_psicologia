package com.psychosim.simulation.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psychosim.domain.user.User;
import com.psychosim.domain.user.UserRepository;
import com.psychosim.domain.user.UserRole;
import com.psychosim.simulation.domain.model.AttemptEventType;
import com.psychosim.simulation.domain.model.AttemptStatus;
import com.psychosim.simulation.domain.model.AttemptToken;
import com.psychosim.simulation.domain.model.CasePublicationStatus;
import com.psychosim.simulation.infrastructure.persistence.AttemptEventEntity;
import com.psychosim.simulation.infrastructure.persistence.AttemptEventJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.CaseVersionEntity;
import com.psychosim.simulation.infrastructure.persistence.CaseVersionJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.DecisionOptionEntity;
import com.psychosim.simulation.infrastructure.persistence.DecisionOptionJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.ReflectionJournalEntity;
import com.psychosim.simulation.infrastructure.persistence.ReflectionJournalJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SimulationAttemptEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationAttemptJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SimulationNodeEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationNodeJpaRepository;
import com.psychosim.simulation.web.SimulationDtos.AttemptCompletionReport;
import com.psychosim.simulation.web.SimulationDtos.AttemptState;
import com.psychosim.simulation.web.SimulationDtos.CaseSummary;
import com.psychosim.simulation.web.SimulationDtos.DecisionOptionState;
import com.psychosim.simulation.web.SimulationDtos.Feedback;
import com.psychosim.simulation.web.SimulationDtos.NodeState;
import com.psychosim.simulation.web.SimulationDtos.ProgressMapNode;
import com.psychosim.simulation.web.SimulationDtos.ProgressMapState;
import com.psychosim.simulation.web.SimulationDtos.ReflectionSaved;
import com.psychosim.simulation.web.SimulationDtos.SimulationMetrics;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import com.psychosim.simulation.infrastructure.audit.Auditable;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SimulationGameService {
    private static final List<String> SAFE_EXIT_RESOURCES = List.of(
            "Puedes pausar el intento y retomarlo con acompañamiento docente.",
            "Si el contenido activa malestar, contacta al Centro Integral de Psicología o a Bienestar Universitario.",
            "En riesgo inmediato, prioriza líneas locales de emergencia y rutas institucionales de protección."
    );

    private final CaseVersionJpaRepository caseVersionRepository;
    private final SimulationNodeJpaRepository nodeRepository;
    private final DecisionOptionJpaRepository decisionRepository;
    private final SimulationAttemptJpaRepository attemptRepository;
    private final AttemptEventJpaRepository eventRepository;
    private final ReflectionJournalJpaRepository reflectionRepository;
    private final UserRepository userRepository;
    private final ReflectionCryptoService reflectionCryptoService;
    private final DecisionEffectCalculator decisionEffectCalculator;
    private final AttemptCompletionReportBuilder completionReportBuilder;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CaseSummary> listPublishedCases() {
        return caseVersionRepository
                .findByStatusAndSimulationCaseActiveTrueOrderByPublishedAtDesc(CasePublicationStatus.PUBLISHED)
                .stream()
                .map(version -> new CaseSummary(
                        version.getId(),
                        version.getSimulationCase().getCode(),
                        version.getSimulationCase().getTitle(),
                        version.getSimulationCase().getDescription(),
                        version.getSemanticVersion(),
                        nodeRepository.findByCaseVersionIdOrderById(version.getId()).size(),
                        version.getStatus().name()
                ))
                .toList();
    }

    @Auditable(action = "ATTEMPT_STARTED", resourceType = "CASE_VERSION")
    @Transactional
    public AttemptState startAttempt(Long caseVersionId, User actor, boolean forceNew) {
        CaseVersionEntity version = requirePublishedCaseVersion(caseVersionId);
        User student = userRepository.findById(actor.getId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        if (!forceNew) {
            Optional<SimulationAttemptEntity> active = attemptRepository
                    .findFirstByStudent_IdAndCaseVersion_IdAndStatusOrderByStartedAtDesc(
                            student.getId(), caseVersionId, AttemptStatus.IN_PROGRESS);
            if (active.isPresent()) {
                return toAttemptState(active.get(), reissueToken(active.get()), null);
            }
        } else {
            closeActiveAttempts(student.getId(), caseVersionId, "Reemplazado por nuevo intento");
        }

        SimulationNodeEntity startNode = nodeRepository.findByCaseVersionIdAndStartNodeTrue(version.getId())
                .orElseThrow(() -> new EntityNotFoundException("El caso no tiene nodo inicial"));

        AttemptToken token = AttemptToken.create();
        SimulationAttemptEntity attempt = new SimulationAttemptEntity();
        attempt.setId(UUID.randomUUID());
        attempt.setAttemptTokenHash(hashToken(token.value()));
        attempt.setCaseVersion(version);
        attempt.setStudent(student);
        attempt.setCurrentNode(startNode);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setAccumulatedScore(0);
        attempt.setStressIndex(0);
        attempt.setVictimRisk(50);
        attempt.setUserTrust(50);
        attempt.setInstitutionalRouteActivated(false);
        attempt.setRevictimizationRisk(false);
        attempt.setStartedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        saveEvent(attempt, AttemptEventType.ATTEMPT_STARTED, startNode, null, 0, 0, "Intento iniciado");
        saveEvent(attempt, AttemptEventType.NODE_ENTERED, startNode, null, 0, 0, "Nodo inicial");

        return toAttemptState(attempt, token.value(), null);
    }

    @Transactional
    public Optional<AttemptState> findActiveAttempt(Long caseVersionId, User actor) {
        requirePublishedCaseVersion(caseVersionId);
        return attemptRepository
                .findFirstByStudent_IdAndCaseVersion_IdAndStatusOrderByStartedAtDesc(
                        actor.getId(), caseVersionId, AttemptStatus.IN_PROGRESS)
                .map(attempt -> toAttemptState(attempt, reissueToken(attempt), null));
    }

    @Transactional(readOnly = true)
    public ProgressMapState getProgressMap(UUID attemptId, String attemptToken, User actor) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId, attemptToken, actor);
        Long caseVersionId = attempt.getCaseVersion().getId();
        List<SimulationNodeEntity> orderedNodes = orderNodesForProgressMap(caseVersionId);
        List<String> visitedNodeKeys = eventRepository.findByAttemptIdOrderByOccurredAt(attemptId).stream()
                .filter(event -> event.getEventType() == AttemptEventType.NODE_ENTERED)
                .map(event -> event.getNode().getNodeKey())
                .distinct()
                .toList();

        List<ProgressMapNode> nodes = orderedNodes.stream()
                .map(node -> new ProgressMapNode(
                        node.getNodeKey(),
                        abbreviateLabel(node.getTitle()),
                        node.isStartNode(),
                        node.isTerminalNode()
                ))
                .toList();

        return new ProgressMapState(nodes, visitedNodeKeys, attempt.getCurrentNode().getNodeKey());
    }

    @Transactional(readOnly = true)
    public AttemptState getAttempt(UUID attemptId, String attemptToken, User actor) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId, attemptToken, actor);
        return toAttemptState(attempt, attemptToken, null);
    }

    @Transactional(readOnly = true)
    public AttemptCompletionReport getCompletionReport(UUID attemptId, String attemptToken, User actor) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId, attemptToken, actor);
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("El intento aun esta en progreso");
        }
        return completionReportBuilder.build(attempt, eventRepository.findByAttemptIdOrderByOccurredAt(attemptId));
    }

    @Auditable(action = "DECISION_SELECTED", resourceType = "ATTEMPT")
    @Transactional
    public AttemptState chooseDecision(UUID attemptId, String attemptToken, Long decisionOptionId, User actor) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId, attemptToken, actor);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("El intento ya no acepta decisiones");
        }

        DecisionOptionEntity decision = decisionRepository.findById(decisionOptionId)
                .orElseThrow(() -> new EntityNotFoundException("Decision no encontrada"));
        if (!decision.getSourceNode().getId().equals(attempt.getCurrentNode().getId())) {
            throw new IllegalArgumentException("La decision no esta disponible desde la escena actual");
        }
        if (!decision.getCaseVersion().getId().equals(attempt.getCaseVersion().getId())) {
            throw new IllegalArgumentException("La decision no pertenece al caso del intento");
        }

        DecisionEffectCalculator.DecisionEffects effects = decisionEffectCalculator.resolve(decision);
        decisionEffectCalculator.apply(attempt, effects);
        AttemptEventType eventType = decision.isProhibitedConduct()
                ? AttemptEventType.PROHIBITED_DECISION_SELECTED
                : AttemptEventType.DECISION_SELECTED;

        attempt.setCurrentNode(decision.getTargetNode());

        saveEvent(attempt, eventType, decision.getSourceNode(), decision, effects.scoreDelta(), effects.stressDelta(),
                decisionEffectCalculator.formatFeedback(decision, effects));
        saveEvent(attempt, AttemptEventType.NODE_ENTERED, decision.getTargetNode(), null, 0, 0, "Nodo visitado");

        if (decision.getTargetNode().isTerminalNode()) {
            attempt.setStatus(AttemptStatus.COMPLETED);
            attempt.setEndedAt(LocalDateTime.now());
            attempt.setLockedAt(LocalDateTime.now());
            lockReflections(attempt);
            saveEvent(attempt, AttemptEventType.ATTEMPT_COMPLETED, decision.getTargetNode(), null, 0, 0, "Intento finalizado");
        }

        Feedback feedback = new Feedback(
                decision.getClassification().name(),
                effects.scoreDelta(),
                effects.stressDelta(),
                effects.trustDelta(),
                effects.victimRiskDelta(),
                decision.isProhibitedConduct(),
                effects.institutionalRouteActivated(),
                effects.revictimizationRisk(),
                decisionEffectCalculator.formatFeedback(decision, effects),
                decision.getProhibitionReason()
        );
        return toAttemptState(attempt, attemptToken, feedback);
    }

    @Auditable(action = "REFLECTION_SAVED", resourceType = "ATTEMPT")
    @Transactional
    public ReflectionSaved saveReflection(UUID attemptId, String attemptToken, Long nodeId, String text, User actor) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId, attemptToken, actor);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("La bitacora ya esta bloqueada");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("La bitacora requiere texto");
        }
        SimulationNodeEntity node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Nodo no encontrado"));
        if (!node.getCaseVersion().getId().equals(attempt.getCaseVersion().getId())) {
            throw new IllegalArgumentException("El nodo no pertenece al caso del intento");
        }

        ReflectionJournalEntity reflection = reflectionRepository.findByAttemptIdAndNodeId(attempt.getId(), node.getId())
                .orElseGet(ReflectionJournalEntity::new);
        if (reflection.isLocked()) {
            throw new IllegalArgumentException("La bitacora ya esta bloqueada");
        }
        reflection.setAttempt(attempt);
        reflection.setNode(node);
        reflection.setEncryptedText(reflectionCryptoService.encrypt(text));
        reflection.setEncryptionKeyRef(reflectionCryptoService.keyRef());
        reflection.setLocked(attempt.getStatus() != AttemptStatus.IN_PROGRESS);
        reflection.setUpdatedAt(LocalDateTime.now());
        reflectionRepository.save(reflection);

        saveEvent(attempt, AttemptEventType.REFLECTION_SAVED, node, null, 0, 0, "Bitacora registrada");
        return new ReflectionSaved(node.getId(), reflection.isLocked());
    }

    @Auditable(action = "SAFE_EXIT_REQUESTED", resourceType = "ATTEMPT")
    @Transactional
    public AttemptState safeExit(UUID attemptId, String attemptToken, String reason, User actor) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId, attemptToken, actor);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return toAttemptState(attempt, attemptToken, null);
        }

        attempt.setStatus(AttemptStatus.SAFE_EXITED);
        attempt.setEndedAt(LocalDateTime.now());
        attempt.setLockedAt(LocalDateTime.now());
        lockReflections(attempt);
        saveEvent(
                attempt,
                AttemptEventType.SAFE_EXIT_REQUESTED,
                attempt.getCurrentNode(),
                null,
                0,
                0,
                reason == null || reason.isBlank() ? "Salida segura solicitada" : reason
        );
        return toAttemptState(attempt, attemptToken, null);
    }

    private CaseVersionEntity requirePublishedCaseVersion(Long caseVersionId) {
        CaseVersionEntity version = caseVersionRepository.findById(caseVersionId)
                .orElseThrow(() -> new EntityNotFoundException("Version de caso no encontrada"));
        if (version.getStatus() != CasePublicationStatus.PUBLISHED || !version.getSimulationCase().isActive()) {
            throw new AccessDeniedException("El caso no esta publicado");
        }
        return version;
    }

    private SimulationAttemptEntity requireAttempt(UUID attemptId, String attemptToken, User actor) {
        if (attemptToken == null || attemptToken.isBlank()) {
            throw new IllegalArgumentException("El token del intento es obligatorio");
        }
        SimulationAttemptEntity attempt = attemptRepository.findByIdAndAttemptTokenHash(attemptId, hashToken(attemptToken))
                .orElseThrow(() -> new EntityNotFoundException("Intento no encontrado"));
        boolean owner = attempt.getStudent().getId().equals(actor.getId());
        boolean staff = actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.PROFESOR;
        if (!owner && !staff) {
            throw new AccessDeniedException("No tiene acceso a este intento");
        }
        return attempt;
    }

    private AttemptState toAttemptState(SimulationAttemptEntity attempt, String rawToken, Feedback feedback) {
        AttemptCompletionReport completionReport = attempt.getStatus() == AttemptStatus.IN_PROGRESS
                ? null
                : completionReportBuilder.build(attempt, eventRepository.findByAttemptIdOrderByOccurredAt(attempt.getId()));
        return new AttemptState(
                attempt.getId(),
                rawToken,
                attempt.getCaseVersion().getId(),
                attempt.getCaseVersion().getSimulationCase().getTitle(),
                attempt.getStatus().name(),
                attempt.getAccumulatedScore(),
                attempt.getStressIndex(),
                completionReportBuilder.toMetrics(attempt),
                toNodeState(attempt.getCurrentNode(), attempt.getStatus() == AttemptStatus.IN_PROGRESS),
                feedback,
                completionReport,
                attempt.getStatus() == AttemptStatus.SAFE_EXITED ? SAFE_EXIT_RESOURCES : List.of()
        );
    }

    private NodeState toNodeState(SimulationNodeEntity node, boolean includeOptions) {
        List<DecisionOptionState> options = includeOptions && !node.isTerminalNode()
                ? decisionRepository.findBySourceNodeIdOrderById(node.getId()).stream()
                .map(option -> new DecisionOptionState(
                        option.getId(),
                        option.getText(),
                        option.getClassification().name(),
                        option.isProhibitedConduct()
                ))
                .toList()
                : List.of();

        return new NodeState(
                node.getId(),
                node.getNodeKey(),
                node.getTitle(),
                node.getNarrative(),
                readStringList(node.getSupportResourcesJson()),
                readStringList(node.getRequiredToolsJson()),
                node.isSensitiveContent(),
                node.isSafeExitRequired(),
                node.getWarningMessage(),
                node.isTerminalNode(),
                options
        );
    }

    private List<String> readStringList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void saveEvent(
            SimulationAttemptEntity attempt,
            AttemptEventType eventType,
            SimulationNodeEntity node,
            DecisionOptionEntity decision,
            int scoreDelta,
            int stressDelta,
            String detail
    ) {
        AttemptEventEntity event = new AttemptEventEntity();
        event.setAttempt(attempt);
        event.setEventType(eventType);
        event.setNode(node);
        event.setDecisionOption(decision);
        event.setScoreDelta(scoreDelta);
        event.setStressDelta(stressDelta);
        event.setDetail(detail);
        event.setOccurredAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    private void lockReflections(SimulationAttemptEntity attempt) {
        reflectionRepository.findByAttemptId(attempt.getId()).forEach(reflection -> {
            reflection.setLocked(true);
            reflection.setUpdatedAt(LocalDateTime.now());
            reflectionRepository.save(reflection);
        });
    }

    private String reissueToken(SimulationAttemptEntity attempt) {
        AttemptToken token = AttemptToken.create();
        attempt.setAttemptTokenHash(hashToken(token.value()));
        attemptRepository.save(attempt);
        return token.value();
    }

    private void closeActiveAttempts(Long studentId, Long caseVersionId, String reason) {
        List<SimulationAttemptEntity> activeAttempts = attemptRepository
                .findByStudent_IdAndCaseVersion_IdAndStatus(studentId, caseVersionId, AttemptStatus.IN_PROGRESS);
        for (SimulationAttemptEntity active : activeAttempts) {
            active.setStatus(AttemptStatus.SAFE_EXITED);
            active.setEndedAt(LocalDateTime.now());
            active.setLockedAt(LocalDateTime.now());
            lockReflections(active);
            saveEvent(
                    active,
                    AttemptEventType.SAFE_EXIT_REQUESTED,
                    active.getCurrentNode(),
                    null,
                    0,
                    0,
                    reason
            );
            attemptRepository.save(active);
        }
    }

    private List<SimulationNodeEntity> orderNodesForProgressMap(Long caseVersionId) {
        List<SimulationNodeEntity> allNodes = nodeRepository.findByCaseVersionIdOrderById(caseVersionId);
        if (allNodes.isEmpty()) {
            return List.of();
        }

        SimulationNodeEntity startNode = nodeRepository.findByCaseVersionIdAndStartNodeTrue(caseVersionId)
                .orElse(allNodes.getFirst());

        Map<Long, List<Long>> adjacency = new LinkedHashMap<>();
        for (SimulationNodeEntity node : allNodes) {
            adjacency.putIfAbsent(node.getId(), new ArrayList<>());
        }
        decisionRepository.findByCaseVersionIdOrderById(caseVersionId).forEach(decision -> {
            adjacency.computeIfAbsent(decision.getSourceNode().getId(), key -> new ArrayList<>())
                    .add(decision.getTargetNode().getId());
        });

        Map<Long, SimulationNodeEntity> byId = new LinkedHashMap<>();
        for (SimulationNodeEntity node : allNodes) {
            byId.put(node.getId(), node);
        }

        List<SimulationNodeEntity> ordered = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        List<Long> queue = new ArrayList<>();
        queue.add(startNode.getId());

        while (!queue.isEmpty()) {
            Long currentId = queue.removeFirst();
            if (!visited.add(currentId)) {
                continue;
            }
            SimulationNodeEntity current = byId.get(currentId);
            if (current != null) {
                ordered.add(current);
            }
            for (Long nextId : adjacency.getOrDefault(currentId, List.of())) {
                if (!visited.contains(nextId)) {
                    queue.add(nextId);
                }
            }
        }

        for (SimulationNodeEntity node : allNodes) {
            if (!visited.contains(node.getId())) {
                ordered.add(node);
            }
        }
        return ordered;
    }

    private static String abbreviateLabel(String title) {
        if (title == null || title.isBlank()) {
            return "Nodo";
        }
        String trimmed = title.trim();
        return trimmed.length() <= 14 ? trimmed : trimmed.substring(0, 13) + "…";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo proteger el token del intento", ex);
        }
    }
}
