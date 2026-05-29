export interface SimulationCaseSummary {
  caseVersionId: number;
  code: string;
  title: string;
  description: string;
  semanticVersion: string;
  nodeCount: number;
  status: string;
}

export interface ProgressMapNode {
  key: string;
  label: string;
  start: boolean;
  terminal: boolean;
}

export interface ProgressMapState {
  nodes: ProgressMapNode[];
  visitedNodeKeys: string[];
  currentNodeKey: string;
}

export interface SimulationAttemptState {
  attemptId: string;
  attemptToken: string;
  caseVersionId: number;
  caseTitle: string;
  status: 'IN_PROGRESS' | 'SAFE_EXITED' | 'COMPLETED';
  accumulatedScore: number;
  stressIndex: number;
  metrics: SimulationMetrics;
  currentNode: SimulationNodeState;
  feedback: SimulationFeedback | null;
  completionReport: AttemptCompletionReport | null;
  supportResources: string[];
}

export interface SimulationMetrics {
  professionalScore: number;
  sceneStress: number;
  victimRisk: number;
  userTrust: number;
  institutionalRouteActivated: boolean;
  revictimizationRisk: boolean;
}

export interface AttemptCompletionReport {
  attemptId: string;
  caseTitle: string;
  status: SimulationAttemptState['status'];
  finalScore: number;
  finalStress: number;
  metrics: SimulationMetrics;
  adequateDecisions: number;
  riskyDecisions: number;
  inadequateDecisions: number;
  prohibitedDecisions: number;
  toolsUsed: number;
  reflectionsCount: number;
  safeExitUsed: boolean;
  visitedNodeTitles: string[];
  competencies: string[];
  recommendations: string[];
  summaryMessage: string;
}

export interface SimulationNodeState {
  id: number;
  key: string;
  title: string;
  narrative: string;
  supportResources: string[];
  requiredTools: string[];
  sensitiveContent: boolean;
  safeExitRequired: boolean;
  warningMessage: string | null;
  terminal: boolean;
  options: SimulationDecisionOption[];
}

export interface SimulationDecisionOption {
  id: number;
  text: string;
  classification: 'ADEQUATE' | 'RISKY' | 'INADEQUATE';
  prohibitedConduct: boolean;
}

export interface SimulationFeedback {
  classification: 'ADEQUATE' | 'RISKY' | 'INADEQUATE';
  scoreDelta: number;
  stressDelta: number;
  trustDelta: number;
  victimRiskDelta: number;
  prohibitedConduct: boolean;
  institutionalRouteActivated: boolean;
  revictimizationRisk: boolean;
  message: string;
  prohibitionReason: string | null;
}

export interface SimulationWorldState {
  attemptId: string;
  status: SimulationAttemptState['status'];
  map: SceneMapState;
  player: PlayerState;
  objects: MapObjectState[];
  collisions: CollisionZoneState[];
  tools: ClinicalToolState[];
  inventory: string[];
  inspectedObjectKeys: string[];
  viewedDialogueKeys: string[];
  usedToolKeys: string[];
  flags: Record<string, unknown>;
}

export interface SceneMapState {
  id: number;
  key: string;
  title: string;
  width: number;
  height: number;
  theme: string;
  spawnX: number;
  spawnY: number;
  ambient: Record<string, unknown>;
}

export interface PlayerState {
  x: number;
  y: number;
}

export interface MapObjectState {
  key: string;
  label: string;
  type: 'PERSON' | 'OBJECT' | 'ROUTE' | 'TOOL' | 'WARNING' | 'EXIT';
  x: number;
  y: number;
  width: number;
  height: number;
  color: string;
  icon: string;
  shortCode: string;
  collision: boolean;
  interactionPrompt: string;
  interactionText: string;
  decisionOptionId: number | null;
  toolCode: string | null;
  dialogue: DialogueState | null;
}

export interface CollisionZoneState {
  key: string;
  label: string | null;
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface ClinicalToolState {
  code: string;
  label: string;
  icon: string;
  category: string;
  description: string;
  active: boolean;
}

export interface DialogueState {
  key: string;
  speakerName: string;
  portraitKey: string | null;
  emotion: string;
  lines: DialogueLineState[];
  choices: DialogueChoiceState[];
}

export interface DialogueLineState {
  order: number;
  speakerName: string;
  text: string;
  emotion: string;
}

export interface DialogueChoiceState {
  key: string;
  text: string;
  decisionOptionId: number | null;
  requiredToolCode: string | null;
  effect: Record<string, unknown>;
  /** UI hint: highlight choice as clinically recommended (optional, backend may omit) */
  isRecommended?: boolean;
  /** UI hint: highlight choice as clinically prohibited (optional, backend may omit) */
  isProhibited?: boolean;
}

export interface InteractionResult {
  world: SimulationWorldState;
  interaction: MapObjectState;
  dialogue: DialogueState | null;
  preparedDecisionOptionId: number | null;
  unlockedToolCode: string | null;
}

/**
 * Fase 6 — Contextual feedback when a clinical tool is used.
 * The stress delta and feedback message make the tool usage diegetic.
 */
export interface ToolUseResult {
  world: SimulationWorldState;
  toolCode: string;
  targetKey: string | null;
  pertinent: boolean;
  stressDelta: number;
  feedbackMessage: string;
}

export interface AttemptTrace {
  attemptId: string;
  studentAlias: string;
  caseTitle: string;
  status: string;
  accumulatedScore: number;
  stressIndex: number;
  metrics: SimulationMetrics;
  startedAt: string;
  endedAt: string | null;
  adequateDecisions: number;
  riskyDecisions: number;
  inadequateDecisions: number;
  prohibitedDecisions: number;
  safeExitUsed: boolean;
  events: TraceEvent[];
  world: SimulationWorldState;
  reflections: ReflectionTrace[];
  rubricEvaluations: RubricSummary[];
}

export interface TraceEvent {
  type: string;
  classification: string | null;
  nodeTitle: string | null;
  decisionText: string | null;
  scoreDelta: number;
  stressDelta: number;
  detail: string | null;
  occurredAt: string;
}

export interface ReflectionTrace {
  nodeId: number;
  nodeTitle: string;
  text: string;
  locked: boolean;
}

export interface RubricSummary {
  id: number;
  rubricName: string;
  totalScore: number;
  comment: string | null;
  evaluatedAt: string;
}

export interface RecentAttempt {
  attemptId: string;
  studentAlias: string;
  caseTitle: string;
  status: string;
  accumulatedScore: number;
  stressIndex: number;
  startedAt: string;
}

export interface RubricEvaluationView {
  rubricId: number;
  rubricName: string;
  description: string | null;
  criteria: RubricCriterionView[];
  scores: CriterionScoreView[];
  totalScore: number | null;
  comment: string | null;
}

export interface RubricCriterionView {
  id: number;
  competency: string;
  title: string;
  description: string | null;
  maxScore: number;
  displayOrder: number;
}

export interface CriterionScoreView {
  criterionId: number;
  score: number;
  comment: string | null;
  evidence: Record<string, unknown>;
}

// ─── Editor-specific states (include DB ids for CRUD) ─────────────────────────

export interface NodeEditorState {
  id: number;
  key: string;
  title: string;
  narrative: string;
  supportResources: string[];
  requiredTools: string[];
  sensitiveContent: boolean;
  safeExitRequired: boolean;
  warningMessage: string | null;
  terminal: boolean;
  startNode: boolean;
  positionX: number | null;
  positionY: number | null;
}

export interface DecisionEdgeState {
  id: number;
  optionKey: string;
  sourceNodeId: number;
  sourceKey: string;
  targetNodeId: number;
  targetKey: string;
  text: string;
  classification: 'ADEQUATE' | 'RISKY' | 'INADEQUATE';
  prohibitedConduct: boolean;
  prohibitionReason: string | null;
  scoreDelta: number;
  stressDelta: number;
  prohibitedPenalty: number;
  immediateFeedback: string;
}

export interface MapEditorState {
  id: number;
  key: string;
  title: string;
  width: number;
  height: number;
  theme: string;
  spawnX: number;
  spawnY: number;
  nodeId: number;
  nodeKey: string;
}

export interface MapObjectEditorState {
  id: number;
  key: string;
  label: string;
  type: string;
  x: number;
  y: number;
  width: number;
  height: number;
  colorHex: string;
  icon: string;
  shortCode: string;
  collision: boolean;
  visible: boolean;
  interactionPrompt: string;
  interactionText: string;
  decisionOptionId: number | null;
  toolCode: string | null;
  mapId: number;
}

export interface ClinicalToolEditorState {
  id: number;
  code: string;
  label: string;
  icon: string;
  category: string;
  description: string;
  active: boolean;
}

export interface CaseEditorView {
  caseVersionId: number;
  title: string;
  semanticVersion: string;
  status: string;
  nodes: NodeEditorState[];
  decisions: DecisionEdgeState[];
  maps: MapEditorState[];
  objects: MapObjectEditorState[];
  tools: ClinicalToolEditorState[];
  rubrics: RubricEvaluationView[];
  checklistCompletion: number;
  publishable: boolean;
}

// ─── Authoring CRUD request models ────────────────────────────────────────────

export interface NodeUpsertRequest {
  nodeKey: string;
  title: string;
  narrative: string;
  requiredTools: string[];
  supportResources: string[];
  sensitiveContent: boolean;
  safeExitRequired: boolean;
  warningMessage: string | null;
  terminal: boolean;
  startNode: boolean;
  positionX: number | null;
  positionY: number | null;
}

export interface DecisionOptionUpsertRequest {
  sourceNodeId: number;
  targetNodeId: number;
  optionKey: string;
  text: string;
  classification: string;
  prohibitedConduct: boolean;
  prohibitionReason: string | null;
  scoreDelta: number;
  stressDelta: number;
  prohibitedPenalty: number;
  immediateFeedback: string;
}

export interface MapUpsertRequest {
  nodeId: number;
  mapKey: string;
  title: string;
  width: number;
  height: number;
  theme: string;
  spawnX: number;
  spawnY: number;
}

export interface MapObjectUpsertRequest {
  objectKey: string;
  label: string;
  objectType: string;
  x: number;
  y: number;
  width: number;
  height: number;
  colorHex: string;
  icon: string;
  shortCode: string;
  collision: boolean;
  visible: boolean;
  interactionPrompt: string;
  interactionText: string;
  decisionOptionId: number | null;
  toolCode: string | null;
}

export interface ToolUpsertRequest {
  toolCode: string;
  label: string;
  icon: string;
  category: string;
  description: string;
}

export interface ChecklistUpdateRequest {
  contentOriginal: boolean;
  ethicsReviewed: boolean;
  safetyProtocols: boolean;
  noStigmatizing: boolean;
  triggerWarnings: boolean;
  accessibilityOk: boolean;
}

// ─── WorldDefinition v2 — contrato canónico entre editor y juego (Fase 2) ────

export interface WorldValidationIssue {
  severity: 'ERROR' | 'WARNING';
  code: string;
  message: string;
  entityRef: string | null;
}

export interface WorldValidationState {
  errors: WorldValidationIssue[];
  warnings: WorldValidationIssue[];
  canPublish: boolean;
}

export interface SceneMapDefinition {
  id: number;
  key: string;
  title: string;
  width: number;
  height: number;
  theme: string;
  spawnX: number;
  spawnY: number;
  ambient: Record<string, unknown>;
}

export type WorldObjectType = 'PERSON' | 'PROP' | 'TOOL_TARGET' | 'EXIT' | 'TRIGGER' | 'NOTE' | 'RESOURCE'
  | 'OBJECT' | 'ROUTE' | 'TOOL' | 'WARNING';

export interface WorldObject {
  id: number;
  key: string;
  label: string;
  type: WorldObjectType;
  x: number;
  y: number;
  width: number;
  height: number;
  zIndex: number;
  facing: 'down' | 'up' | 'left' | 'right';
  colorHex: string;
  icon: string;
  shortCode: string;
  collision: boolean;
  visible: boolean;
  interactionPrompt: string;
  interactionText: string;
  decisionOptionId: number | null;
  toolCode: string | null;
  unlockCondition: Record<string, unknown>;
  movementPattern: Record<string, unknown>;
  metadata: Record<string, unknown>;
}

export interface WorldCollisionZone {
  id: number;
  key: string;
  label: string | null;
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface WorldDialogueLine {
  order: number;
  speakerName: string;
  text: string;
  emotion: string;
}

export interface WorldDialogueChoice {
  key: string;
  text: string;
  decisionOptionId: number | null;
  requiredToolCode: string | null;
  effect: Record<string, unknown>;
  displayOrder: number;
}

export interface WorldDialogueTree {
  id: number;
  key: string;
  speakerName: string;
  portraitKey: string | null;
  emotion: string;
  mapObjectId: number | null;
  lines: WorldDialogueLine[];
  choices: WorldDialogueChoice[];
}

export interface WorldClinicalTool {
  id: number;
  code: string;
  label: string;
  icon: string;
  category: string;
  description: string;
  active: boolean;
}

export interface SafeExitConfig {
  configured: boolean;
  exitObjectKey: string | null;
  supportResources: string[];
}

export interface WorldDefinition {
  schemaVersion: number;
  caseVersionId: number;
  revision: number;
  nodeId: number;
  map: SceneMapDefinition;
  objects: WorldObject[];
  collisionZones: WorldCollisionZone[];
  dialogues: WorldDialogueTree[];
  clinicalTools: WorldClinicalTool[];
  safeExit: SafeExitConfig;
  validation: WorldValidationState;
}

export interface WorldSaveRequest {
  revision: number;
  map: SceneMapDefinition;
  objects: WorldObject[];
  collisionZones: WorldCollisionZone[];
  dialogues: WorldDialogueTree[];
  clinicalTools: WorldClinicalTool[];
}
