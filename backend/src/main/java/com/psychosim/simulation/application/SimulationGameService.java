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
import com.psychosim.simulation.web.SimulationDtos.AttemptState;
import com.psychosim.simulation.web.SimulationDtos.CaseSummary;
import com.psychosim.simulation.web.SimulationDtos.DecisionOptionState;
import com.psychosim.simulation.web.SimulationDtos.Feedback;
import com.psychosim.simulation.web.SimulationDtos.NodeState;
import com.psychosim.simulation.web.SimulationDtos.ReflectionSaved;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import com.psychosim.simulation.infrastructure.audit.Auditable;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
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
    private final SimulationWorldService simulationWorldService;
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
    public AttemptState startAttempt(Long caseVersionId, User actor) {
        CaseVersionEntity version = requirePublishedCaseVersion(caseVersionId);
        User student = userRepository.findById(actor.getId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
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
        attempt.setStartedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        saveEvent(attempt, AttemptEventType.ATTEMPT_STARTED, startNode, null, 0, 0, "Intento iniciado");
        saveEvent(attempt, AttemptEventType.NODE_ENTERED, startNode, null, 0, 0, "Nodo inicial");

        return toAttemptState(attempt, token.value(), null);
    }

    @Transactional(readOnly = true)
    public AttemptState getAttempt(UUID attemptId, String attemptToken, User actor) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId, attemptToken, actor);
        return toAttemptState(attempt, attemptToken, null);
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

        int scoreDelta = decision.getScoreDelta() + (decision.isProhibitedConduct() ? decision.getProhibitedPenalty() : 0);
        int stressDelta = decision.getStressDelta() + (decision.isProhibitedConduct() ? 40 : 0);
        AttemptEventType eventType = decision.isProhibitedConduct()
                ? AttemptEventType.PROHIBITED_DECISION_SELECTED
                : AttemptEventType.DECISION_SELECTED;

        attempt.setAccumulatedScore(attempt.getAccumulatedScore() + scoreDelta);
        attempt.setStressIndex(clamp(attempt.getStressIndex() + stressDelta, 0, 100));
        attempt.setCurrentNode(decision.getTargetNode());

        saveEvent(attempt, eventType, decision.getSourceNode(), decision, scoreDelta, stressDelta, decision.getImmediateFeedback());
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
                scoreDelta,
                stressDelta,
                decision.isProhibitedConduct(),
                decision.getImmediateFeedback(),
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
        return new AttemptState(
                attempt.getId(),
                rawToken,
                attempt.getCaseVersion().getId(),
                attempt.getCaseVersion().getSimulationCase().getTitle(),
                attempt.getStatus().name(),
                attempt.getAccumulatedScore(),
                attempt.getStressIndex(),
                toNodeState(attempt.getCurrentNode(), attempt.getStatus() == AttemptStatus.IN_PROGRESS),
                feedback,
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
