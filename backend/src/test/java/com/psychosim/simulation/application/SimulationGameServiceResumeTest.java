package com.psychosim.simulation.application;

import com.psychosim.domain.user.User;
import com.psychosim.domain.user.UserRepository;
import com.psychosim.simulation.domain.model.AttemptStatus;
import com.psychosim.simulation.domain.model.CasePublicationStatus;
import com.psychosim.simulation.infrastructure.persistence.CaseVersionEntity;
import com.psychosim.simulation.infrastructure.persistence.CaseVersionJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SimulationAttemptEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationAttemptJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SimulationCaseEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationNodeEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationNodeJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.AttemptEventJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.DecisionOptionJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.ReflectionJournalJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationGameServiceResumeTest {

    @Mock private CaseVersionJpaRepository caseVersionRepository;
    @Mock private SimulationNodeJpaRepository nodeRepository;
    @Mock private DecisionOptionJpaRepository decisionRepository;
    @Mock private SimulationAttemptJpaRepository attemptRepository;
    @Mock private AttemptEventJpaRepository eventRepository;
    @Mock private ReflectionJournalJpaRepository reflectionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReflectionCryptoService reflectionCryptoService;
    @Mock private DecisionEffectCalculator decisionEffectCalculator;
    @Mock private AttemptCompletionReportBuilder completionReportBuilder;

    @InjectMocks
    private SimulationGameService simulationGameService;

    private User student;
    private CaseVersionEntity version;
    private SimulationAttemptEntity activeAttempt;

    @BeforeEach
    void setUp() {
        simulationGameService = new SimulationGameService(
                caseVersionRepository,
                nodeRepository,
                decisionRepository,
                attemptRepository,
                eventRepository,
                reflectionRepository,
                userRepository,
                reflectionCryptoService,
                decisionEffectCalculator,
                completionReportBuilder,
                new ObjectMapper()
        );

        student = new User();
        student.setId(10L);
        student.setEmail("estudiante@psychosim.edu.co");

        SimulationCaseEntity simulationCase = new SimulationCaseEntity();
        simulationCase.setTitle("Caso demo");
        simulationCase.setActive(true);

        version = new CaseVersionEntity();
        version.setId(1L);
        version.setStatus(CasePublicationStatus.PUBLISHED);
        version.setSimulationCase(simulationCase);

        SimulationNodeEntity currentNode = new SimulationNodeEntity();
        currentNode.setId(100L);
        currentNode.setNodeKey("node-a");
        currentNode.setTitle("Escena A");
        currentNode.setTerminalNode(false);

        activeAttempt = new SimulationAttemptEntity();
        activeAttempt.setId(UUID.randomUUID());
        activeAttempt.setAttemptTokenHash("old-hash");
        activeAttempt.setCaseVersion(version);
        activeAttempt.setStudent(student);
        activeAttempt.setCurrentNode(currentNode);
        activeAttempt.setStatus(AttemptStatus.IN_PROGRESS);
        activeAttempt.setAccumulatedScore(20);
        activeAttempt.setStressIndex(15);
        activeAttempt.setVictimRisk(45);
        activeAttempt.setUserTrust(55);
    }

    @Test
    void findActiveAttemptReissuesTokenForSameAttempt() {
        when(caseVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(attemptRepository.findFirstByStudent_IdAndCaseVersion_IdAndStatusOrderByStartedAtDesc(
                10L, 1L, AttemptStatus.IN_PROGRESS)).thenReturn(Optional.of(activeAttempt));
        when(attemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = simulationGameService.findActiveAttempt(1L, student);

        assertTrue(result.isPresent());
        assertEquals(activeAttempt.getId(), result.get().attemptId());
        verify(attemptRepository).save(activeAttempt);
    }

    @Test
    void startAttemptWithoutForceNewReusesActiveAttempt() {
        when(caseVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(userRepository.findById(10L)).thenReturn(Optional.of(student));
        when(attemptRepository.findFirstByStudent_IdAndCaseVersion_IdAndStatusOrderByStartedAtDesc(
                10L, 1L, AttemptStatus.IN_PROGRESS)).thenReturn(Optional.of(activeAttempt));
        when(attemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = simulationGameService.startAttempt(1L, student, false);

        assertEquals(activeAttempt.getId(), result.attemptId());
        verify(attemptRepository).save(activeAttempt);
        verify(nodeRepository, never()).findByCaseVersionIdAndStartNodeTrue(any());
    }

    @Test
    void startAttemptWithForceNewCreatesFreshAttempt() {
        SimulationNodeEntity startNode = new SimulationNodeEntity();
        startNode.setId(1L);
        startNode.setNodeKey("start");
        startNode.setTitle("Inicio");
        startNode.setTerminalNode(false);

        when(caseVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(userRepository.findById(10L)).thenReturn(Optional.of(student));
        when(attemptRepository.findByStudent_IdAndCaseVersion_IdAndStatus(
                10L, 1L, AttemptStatus.IN_PROGRESS)).thenReturn(List.of(activeAttempt));
        when(nodeRepository.findByCaseVersionIdAndStartNodeTrue(1L)).thenReturn(Optional.of(startNode));
        when(attemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = simulationGameService.startAttempt(1L, student, true);

        assertTrue(result.attemptToken() != null && !result.attemptToken().isBlank());
        ArgumentCaptor<SimulationAttemptEntity> captor = ArgumentCaptor.forClass(SimulationAttemptEntity.class);
        verify(attemptRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        SimulationAttemptEntity created = captor.getAllValues().get(1);
        assertEquals(AttemptStatus.IN_PROGRESS, created.getStatus());
        assertEquals(AttemptStatus.SAFE_EXITED, activeAttempt.getStatus());
    }
}
