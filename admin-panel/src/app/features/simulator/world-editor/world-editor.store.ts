/**
 * WorldEditorStore — Fase 4: Editor de mundo MVP
 *
 * Pure Signals-based state management with undo/redo command stack.
 * No NgRx — uses Angular Signals for reactive state + command pattern.
 *
 * Responsibilities:
 *   - Load WorldDefinition from backend
 *   - Track local edits via command stack (undo/redo)
 *   - Auto-save with debounce → PUT /world with revision
 *   - Handle 409 Conflict (optimistic lock) without silent data loss
 *   - Run validation on demand
 */
import { Injectable, inject, signal, computed } from '@angular/core';
import { Subject, debounceTime, switchMap, catchError, of, EMPTY } from 'rxjs';
import { SimulationService } from '../../../core/api/simulation.service';
import {
  WorldDefinition,
  WorldObject,
  WorldCollisionZone,
  WorldValidationState,
  WorldSaveRequest,
  SceneMapDefinition
} from '../../../core/models/simulation.model';

// ─── Command Pattern ────────────────────────────────────────────────────────

export interface EditorCommand {
  readonly type: string;
  execute(state: EditorState): EditorState;
  undo(state: EditorState): EditorState;
}

export class PlaceObjectCommand implements EditorCommand {
  readonly type = 'PlaceObject';
  constructor(private readonly object: WorldObject) {}

  execute(state: EditorState): EditorState {
    return { ...state, objects: [...state.objects, this.object] };
  }

  undo(state: EditorState): EditorState {
    return { ...state, objects: state.objects.filter(o => o.key !== this.object.key) };
  }
}

export class MoveObjectCommand implements EditorCommand {
  readonly type = 'MoveObject';
  constructor(
    private readonly key: string,
    private readonly fromX: number,
    private readonly fromY: number,
    private readonly toX: number,
    private readonly toY: number
  ) {}

  execute(state: EditorState): EditorState {
    return {
      ...state,
      objects: state.objects.map(o =>
        o.key === this.key ? { ...o, x: this.toX, y: this.toY } : o
      )
    };
  }

  undo(state: EditorState): EditorState {
    return {
      ...state,
      objects: state.objects.map(o =>
        o.key === this.key ? { ...o, x: this.fromX, y: this.fromY } : o
      )
    };
  }
}

export class ResizeZoneCommand implements EditorCommand {
  readonly type = 'ResizeZone';
  constructor(
    private readonly key: string,
    private readonly from: { x: number; y: number; width: number; height: number },
    private readonly to: { x: number; y: number; width: number; height: number }
  ) {}

  execute(state: EditorState): EditorState {
    return {
      ...state,
      collisionZones: state.collisionZones.map(z =>
        z.key === this.key ? { ...z, ...this.to } : z
      )
    };
  }

  undo(state: EditorState): EditorState {
    return {
      ...state,
      collisionZones: state.collisionZones.map(z =>
        z.key === this.key ? { ...z, ...this.from } : z
      )
    };
  }
}

export class DeleteObjectCommand implements EditorCommand {
  readonly type = 'DeleteObject';
  private deleted: WorldObject | null = null;

  constructor(private readonly key: string) {}

  execute(state: EditorState): EditorState {
    this.deleted = state.objects.find(o => o.key === this.key) ?? null;
    return { ...state, objects: state.objects.filter(o => o.key !== this.key) };
  }

  undo(state: EditorState): EditorState {
    if (!this.deleted) return state;
    return { ...state, objects: [...state.objects, this.deleted] };
  }
}

export class UpdateInspectorCommand implements EditorCommand {
  readonly type = 'UpdateInspector';
  private previous: WorldObject | null = null;

  constructor(private readonly key: string, private readonly updates: Partial<WorldObject>) {}

  execute(state: EditorState): EditorState {
    this.previous = state.objects.find(o => o.key === this.key) ?? null;
    return {
      ...state,
      objects: state.objects.map(o =>
        o.key === this.key ? { ...o, ...this.updates } : o
      )
    };
  }

  undo(state: EditorState): EditorState {
    if (!this.previous) return state;
    return {
      ...state,
      objects: state.objects.map(o =>
        o.key === this.key ? this.previous! : o
      )
    };
  }
}

export class PlaceCollisionZoneCommand implements EditorCommand {
  readonly type = 'PlaceCollisionZone';
  constructor(private readonly zone: WorldCollisionZone) {}

  execute(state: EditorState): EditorState {
    return { ...state, collisionZones: [...state.collisionZones, this.zone] };
  }

  undo(state: EditorState): EditorState {
    return { ...state, collisionZones: state.collisionZones.filter(z => z.key !== this.zone.key) };
  }
}

export class DeleteCollisionZoneCommand implements EditorCommand {
  readonly type = 'DeleteCollisionZone';
  private deleted: WorldCollisionZone | null = null;

  constructor(private readonly key: string) {}

  execute(state: EditorState): EditorState {
    this.deleted = state.collisionZones.find(z => z.key === this.key) ?? null;
    return { ...state, collisionZones: state.collisionZones.filter(z => z.key !== this.key) };
  }

  undo(state: EditorState): EditorState {
    if (!this.deleted) return state;
    return { ...state, collisionZones: [...state.collisionZones, this.deleted] };
  }
}

export class UpdateSpawnCommand implements EditorCommand {
  readonly type = 'UpdateSpawn';
  private previousX = 0;
  private previousY = 0;

  constructor(private readonly x: number, private readonly y: number) {}

  execute(state: EditorState): EditorState {
    this.previousX = state.map.spawnX;
    this.previousY = state.map.spawnY;
    return { ...state, map: { ...state.map, spawnX: this.x, spawnY: this.y } };
  }

  undo(state: EditorState): EditorState {
    return { ...state, map: { ...state.map, spawnX: this.previousX, spawnY: this.previousY } };
  }
}

// ─── Editor State ───────────────────────────────────────────────────────────

export interface EditorState {
  map: SceneMapDefinition;
  objects: WorldObject[];
  collisionZones: WorldCollisionZone[];
}

export type EditorTool = 'select' | 'place-object' | 'place-collision' | 'place-spawn' | 'pan';

// ─── Store ──────────────────────────────────────────────────────────────────

@Injectable()
export class WorldEditorStore {
  private readonly simulationService = inject(SimulationService);

  // ─── Source of truth ───────────────────────────────────────────────────
  readonly definition = signal<WorldDefinition | null>(null);
  readonly editorState = signal<EditorState | null>(null);
  readonly selectedKey = signal<string | null>(null);
  readonly activeTool = signal<EditorTool>('select');
  readonly zoom = signal(1);
  readonly panOffset = signal({ x: 0, y: 0 });

  // ─── Command stack ────────────────────────────────────────────────────
  private undoStack: EditorCommand[] = [];
  private redoStack: EditorCommand[] = [];
  readonly canUndo = signal(false);
  readonly canRedo = signal(false);

  // ─── Status ───────────────────────────────────────────────────────────
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly dirty = signal(false);
  readonly error = signal('');
  readonly conflictDetected = signal(false);
  readonly validationState = signal<WorldValidationState | null>(null);

  // ─── Auto-save debounce ───────────────────────────────────────────────
  private readonly saveSubject = new Subject<void>();
  private caseVersionId = 0;
  private nodeId: number | undefined;

  // ─── Computed ─────────────────────────────────────────────────────────
  readonly selectedObject = computed(() => {
    const key = this.selectedKey();
    const state = this.editorState();
    if (!key || !state) return null;
    return state.objects.find(o => o.key === key) ?? null;
  });

  readonly selectedZone = computed(() => {
    const key = this.selectedKey();
    const state = this.editorState();
    if (!key || !state) return null;
    return state.collisionZones.find(z => z.key === key) ?? null;
  });

  readonly objectCount = computed(() => this.editorState()?.objects.length ?? 0);
  readonly zoneCount = computed(() => this.editorState()?.collisionZones.length ?? 0);

  constructor() {
    // Auto-save pipeline: debounce 1500ms → PUT /world
    this.saveSubject.pipe(
      debounceTime(1500),
      switchMap(() => this.executeSave()),
      catchError(err => {
        if (err?.status === 409) {
          this.conflictDetected.set(true);
          this.error.set('Este caso cambio en otra sesion. Tus cambios locales se conservan. Recarga para ver la version mas reciente.');
        } else {
          this.error.set('No se pudo guardar el borrador.');
        }
        this.saving.set(false);
        return EMPTY;
      })
    ).subscribe(def => {
      if (def) {
        this.definition.set(def);
        this.saving.set(false);
        this.dirty.set(false);
      }
    });
  }

  // ─── Load ─────────────────────────────────────────────────────────────

  load(caseVersionId: number, nodeId?: number): void {
    this.caseVersionId = caseVersionId;
    this.nodeId = nodeId;
    this.loading.set(true);
    this.error.set('');
    this.conflictDetected.set(false);
    this.resetCommandStack();

    this.simulationService.worldEditor(caseVersionId, nodeId).subscribe({
      next: def => {
        this.definition.set(def);
        this.editorState.set({
          map: { ...def.map },
          objects: [...def.objects],
          collisionZones: [...def.collisionZones]
        });
        this.validationState.set(def.validation);
        this.loading.set(false);
        this.dirty.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el mundo para edicion.');
        this.loading.set(false);
      }
    });
  }

  // ─── Command execution ────────────────────────────────────────────────

  execute(command: EditorCommand): void {
    const current = this.editorState();
    if (!current) return;

    const next = command.execute(current);
    this.editorState.set(next);
    this.undoStack.push(command);
    this.redoStack = []; // Clear redo on new action
    this.updateStackSignals();
    this.markDirty();
  }

  undo(): void {
    const command = this.undoStack.pop();
    if (!command) return;
    const current = this.editorState();
    if (!current) return;

    const prev = command.undo(current);
    this.editorState.set(prev);
    this.redoStack.push(command);
    this.updateStackSignals();
    this.markDirty();
  }

  redo(): void {
    const command = this.redoStack.pop();
    if (!command) return;
    const current = this.editorState();
    if (!current) return;

    const next = command.execute(current);
    this.editorState.set(next);
    this.undoStack.push(command);
    this.updateStackSignals();
    this.markDirty();
  }

  // ─── Selection ────────────────────────────────────────────────────────

  select(key: string | null): void {
    this.selectedKey.set(key);
  }

  // ─── Tools ────────────────────────────────────────────────────────────

  setTool(tool: EditorTool): void {
    this.activeTool.set(tool);
    if (tool !== 'select') this.selectedKey.set(null);
  }

  // ─── Zoom/Pan ─────────────────────────────────────────────────────────

  setZoom(z: number): void {
    this.zoom.set(Math.max(0.25, Math.min(4, z)));
  }

  setPan(x: number, y: number): void {
    this.panOffset.set({ x, y });
  }

  // ─── Save (manual) ───────────────────────────────────────────────────

  saveNow(): void {
    this.saveSubject.next();
  }

  // ─── Validate ─────────────────────────────────────────────────────────

  validate(): void {
    this.simulationService.validateWorld(this.caseVersionId).subscribe({
      next: vs => this.validationState.set(vs),
      error: () => this.error.set('No se pudo ejecutar la validacion.')
    });
  }

  // ─── Reload after conflict ────────────────────────────────────────────

  reloadAfterConflict(): void {
    this.conflictDetected.set(false);
    this.load(this.caseVersionId, this.nodeId);
  }

  // ─── Helpers ──────────────────────────────────────────────────────────

  /** Generate a unique key for new objects. */
  generateKey(prefix: string): string {
    return `${prefix}-${Date.now().toString(36)}`;
  }

  /** Next sequential ID (local only, backend assigns real IDs on save). */
  nextLocalId(): number {
    const state = this.editorState();
    if (!state) return 1;
    const maxObj = Math.max(0, ...state.objects.map(o => o.id));
    const maxZone = Math.max(0, ...state.collisionZones.map(z => z.id));
    return Math.max(maxObj, maxZone) + 1;
  }

  // ─── Private ──────────────────────────────────────────────────────────

  private markDirty(): void {
    this.dirty.set(true);
    this.saveSubject.next(); // Trigger debounced auto-save
  }

  private executeSave() {
    const def = this.definition();
    const state = this.editorState();
    if (!def || !state) return of(null);

    this.saving.set(true);
    const body: WorldSaveRequest = {
      revision: def.revision,
      map: state.map,
      objects: state.objects,
      collisionZones: state.collisionZones,
      dialogues: def.dialogues, // Preserve — not edited in the visual canvas
      clinicalTools: def.clinicalTools // Preserve — edited in the CRUD tab
    };
    return this.simulationService.saveWorld(this.caseVersionId, body, this.nodeId);
  }

  private resetCommandStack(): void {
    this.undoStack = [];
    this.redoStack = [];
    this.updateStackSignals();
  }

  private updateStackSignals(): void {
    this.canUndo.set(this.undoStack.length > 0);
    this.canRedo.set(this.redoStack.length > 0);
  }
}
