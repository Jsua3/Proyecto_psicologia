/**
 * WorldEditorComponent — Fase 4: Editor de mundo MVP (Konva + Angular Signals)
 *
 * Visual authoring of world maps using Konva canvas:
 *   - Grid/zoom/pan/snap
 *   - Select/move/resize objects and collision zones
 *   - Create objects, persons, collisions, triggers
 *   - Define spawn and safe exit
 *   - Side inspector (liquid-glass)
 *   - Save draft (debounced auto-save + manual)
 *   - Validate button
 *   - Undo/Redo (command stack)
 *
 * Coordinates in PIXELS (Decision #5 — WYSIWYG with Phaser runtime).
 */
import {
  Component,
  ElementRef,
  OnInit,
  OnDestroy,
  ViewChild,
  inject,
  input,
  effect,
  signal,
  HostListener
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import Konva from 'konva';

import {
  WorldEditorStore,
  EditorTool,
  PlaceObjectCommand,
  MoveObjectCommand,
  DeleteObjectCommand,
  UpdateInspectorCommand,
  PlaceCollisionZoneCommand,
  DeleteCollisionZoneCommand,
  ResizeZoneCommand,
  UpdateSpawnCommand
} from './world-editor.store';
import {
  WorldObject,
  WorldObjectType,
  WorldCollisionZone,
  WorldValidationState
} from '../../../core/models/simulation.model';

// ─── Constants ──────────────────────────────────────────────────────────────

const GRID_SIZE = 16;
const SNAP_THRESHOLD = 8;
const OBJECT_COLORS: Record<string, string> = {
  PERSON: '#4f7cac',
  OBJECT: '#4fa3a5',
  PROP: '#4fa3a5',
  TOOL: '#6a8e5e',
  TOOL_TARGET: '#6a8e5e',
  EXIT: '#a85064',
  WARNING: '#c6a850',
  ROUTE: '#7a6f9e',
  TRIGGER: '#9e8f6f',
  NOTE: '#8fa3b8',
  RESOURCE: '#5e8e6a'
};

@Component({
  selector: 'app-world-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatProgressBarModule],
  providers: [WorldEditorStore],
  template: `
    <div class="we-root">
      <!-- ── Toolbar ─────────────────────────────────────────────────── -->
      <header class="we-toolbar liquid-glass">
        <div class="we-tools">
          <button class="we-tool-btn" [class.active]="store.activeTool() === 'select'"
                  (click)="store.setTool('select')" title="Seleccionar (V)">
            <mat-icon>near_me</mat-icon>
          </button>
          <button class="we-tool-btn" [class.active]="store.activeTool() === 'pan'"
                  (click)="store.setTool('pan')" title="Mover lienzo (H)">
            <mat-icon>pan_tool</mat-icon>
          </button>
          <span class="we-divider"></span>
          <button class="we-tool-btn" [class.active]="store.activeTool() === 'place-object'"
                  (click)="store.setTool('place-object')" title="Colocar objeto (O)">
            <mat-icon>add_location_alt</mat-icon>
          </button>
          <button class="we-tool-btn" [class.active]="store.activeTool() === 'place-collision'"
                  (click)="store.setTool('place-collision')" title="Zona de colision (C)">
            <mat-icon>crop_free</mat-icon>
          </button>
          <button class="we-tool-btn" [class.active]="store.activeTool() === 'place-spawn'"
                  (click)="store.setTool('place-spawn')" title="Punto de spawn (S)">
            <mat-icon>my_location</mat-icon>
          </button>
        </div>

        <div class="we-actions">
          <button class="we-tool-btn" [disabled]="!store.canUndo()" (click)="store.undo()" title="Deshacer (Ctrl+Z)">
            <mat-icon>undo</mat-icon>
          </button>
          <button class="we-tool-btn" [disabled]="!store.canRedo()" (click)="store.redo()" title="Rehacer (Ctrl+Y)">
            <mat-icon>redo</mat-icon>
          </button>
          <span class="we-divider"></span>
          <span class="we-zoom-label">{{ (store.zoom() * 100) | number:'1.0-0' }}%</span>
          <button class="we-tool-btn" (click)="zoomIn()" title="Acercar (+)"><mat-icon>zoom_in</mat-icon></button>
          <button class="we-tool-btn" (click)="zoomOut()" title="Alejar (-)"><mat-icon>zoom_out</mat-icon></button>
          <button class="we-tool-btn" (click)="zoomFit()" title="Ajustar (0)"><mat-icon>fit_screen</mat-icon></button>
          <span class="we-divider"></span>
          <button class="psy-button psy-button--glass" (click)="store.validate()" type="button">
            <mat-icon>verified</mat-icon>Validar
          </button>
          <button class="psy-button psy-button--primary" (click)="store.saveNow()" type="button"
                  [disabled]="store.saving() || !store.dirty()">
            <mat-icon>{{ store.saving() ? 'hourglass_empty' : 'save' }}</mat-icon>
            {{ store.saving() ? 'Guardando...' : 'Guardar' }}
          </button>
        </div>

        <div class="we-status">
          @if (store.dirty()) {
            <span class="we-badge we-badge--dirty">Sin guardar</span>
          }
          @if (store.saving()) {
            <span class="we-badge we-badge--saving">Guardando...</span>
          }
          <span class="we-info">
            {{ store.objectCount() }} objetos · {{ store.zoneCount() }} zonas
          </span>
        </div>
      </header>

      @if (store.loading()) {
        <mat-progress-bar mode="indeterminate" />
      }

      @if (store.error()) {
        <div class="we-error" role="alert">
          <mat-icon>error</mat-icon>
          <span>{{ store.error() }}</span>
          @if (store.conflictDetected()) {
            <button class="psy-button psy-button--glass" (click)="store.reloadAfterConflict()" type="button">
              <mat-icon>refresh</mat-icon>Recargar
            </button>
          }
        </div>
      }

      <!-- ── Canvas + Inspector layout ─────────────────────────────── -->
      <div class="we-layout">
        <div class="we-canvas-wrap" #canvasWrap>
          <div #konvaHost class="we-konva-host"></div>
        </div>

        <!-- ── Side Inspector ──────────────────────────────────────── -->
        <aside class="we-inspector liquid-glass">
          @if (store.selectedObject(); as obj) {
            <h4>Objeto: {{ obj.label }}</h4>
            <div class="we-form">
              <label><span>Clave</span><input [ngModel]="obj.key" disabled /></label>
              <label><span>Etiqueta</span>
                <input [ngModel]="obj.label" (ngModelChange)="updateObjectField(obj.key, 'label', $event)" />
              </label>
              <label><span>Tipo</span>
                <select [ngModel]="obj.type" (ngModelChange)="updateObjectField(obj.key, 'type', $event)">
                  <option value="PERSON">Persona</option>
                  <option value="OBJECT">Objeto</option>
                  <option value="PROP">Prop</option>
                  <option value="TOOL">Herramienta</option>
                  <option value="EXIT">Salida</option>
                  <option value="WARNING">Alerta</option>
                  <option value="ROUTE">Ruta</option>
                  <option value="TRIGGER">Trigger</option>
                </select>
              </label>
              <div class="we-form-row">
                <label><span>X</span><input type="number" [ngModel]="obj.x"
                  (ngModelChange)="updateObjectField(obj.key, 'x', $event)" /></label>
                <label><span>Y</span><input type="number" [ngModel]="obj.y"
                  (ngModelChange)="updateObjectField(obj.key, 'y', $event)" /></label>
              </div>
              <div class="we-form-row">
                <label><span>Ancho</span><input type="number" [ngModel]="obj.width"
                  (ngModelChange)="updateObjectField(obj.key, 'width', $event)" /></label>
                <label><span>Alto</span><input type="number" [ngModel]="obj.height"
                  (ngModelChange)="updateObjectField(obj.key, 'height', $event)" /></label>
              </div>
              <label><span>Prompt</span>
                <input [ngModel]="obj.interactionPrompt" (ngModelChange)="updateObjectField(obj.key, 'interactionPrompt', $event)" />
              </label>
              <label><span>Codigo corto</span>
                <input [ngModel]="obj.shortCode" maxlength="12" (ngModelChange)="updateObjectField(obj.key, 'shortCode', $event)" />
              </label>
              <label><span>Color</span>
                <input type="color" [ngModel]="obj.colorHex" (ngModelChange)="updateObjectField(obj.key, 'colorHex', $event)" />
              </label>
              <label class="we-check">
                <input type="checkbox" [ngModel]="obj.collision" (ngModelChange)="updateObjectField(obj.key, 'collision', $event)" />
                <span>Colision activa</span>
              </label>
              <label class="we-check">
                <input type="checkbox" [ngModel]="obj.visible" (ngModelChange)="updateObjectField(obj.key, 'visible', $event)" />
                <span>Visible</span>
              </label>
              <button class="psy-button psy-button--ghost we-delete-btn" (click)="deleteSelected()" type="button">
                <mat-icon>delete_outline</mat-icon>Eliminar objeto
              </button>
            </div>
          } @else if (store.selectedZone(); as zone) {
            <h4>Zona: {{ zone.label || zone.key }}</h4>
            <div class="we-form">
              <label><span>Clave</span><input [ngModel]="zone.key" disabled /></label>
              <label><span>Etiqueta</span>
                <input [ngModel]="zone.label ?? ''" (ngModelChange)="updateZoneLabel(zone.key, $event)" />
              </label>
              <div class="we-form-row">
                <label><span>X</span><input type="number" [ngModel]="zone.x" disabled /></label>
                <label><span>Y</span><input type="number" [ngModel]="zone.y" disabled /></label>
              </div>
              <div class="we-form-row">
                <label><span>Ancho</span><input type="number" [ngModel]="zone.width" disabled /></label>
                <label><span>Alto</span><input type="number" [ngModel]="zone.height" disabled /></label>
              </div>
              <button class="psy-button psy-button--ghost we-delete-btn" (click)="deleteSelectedZone()" type="button">
                <mat-icon>delete_outline</mat-icon>Eliminar zona
              </button>
            </div>
          } @else {
            <div class="we-inspector-empty">
              <mat-icon>touch_app</mat-icon>
              <p>Selecciona un objeto o zona en el lienzo para editar sus propiedades.</p>
              <p class="we-hint">Atajos: V=seleccionar, O=objeto, C=colision, S=spawn, H=mover, Ctrl+Z/Y=undo/redo, Supr=eliminar</p>
            </div>
          }

          <!-- Validation panel -->
          @if (store.validationState(); as vs) {
            <div class="we-validation">
              <h4>
                <mat-icon>{{ vs.canPublish ? 'check_circle' : 'error' }}</mat-icon>
                Validacion
              </h4>
              @if (vs.errors.length) {
                <div class="we-val-section we-val-errors">
                  @for (issue of vs.errors; track issue.code) {
                    <div class="we-val-item">
                      <mat-icon>error</mat-icon>
                      <span>{{ issue.message }}</span>
                    </div>
                  }
                </div>
              }
              @if (vs.warnings.length) {
                <div class="we-val-section we-val-warnings">
                  @for (issue of vs.warnings; track issue.code) {
                    <div class="we-val-item">
                      <mat-icon>warning</mat-icon>
                      <span>{{ issue.message }}</span>
                    </div>
                  }
                </div>
              }
              @if (!vs.errors.length && !vs.warnings.length) {
                <p class="we-val-ok">Sin errores ni advertencias.</p>
              }
            </div>
          }
        </aside>
      </div>
    </div>
  `,
  styles: [`
    .we-root { display: grid; gap: 12px; }

    /* Toolbar */
    .we-toolbar {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      align-items: center;
      padding: 8px 14px;
      border-radius: 16px;
    }
    .we-tools, .we-actions { display: flex; gap: 4px; align-items: center; }
    .we-actions { margin-left: auto; }
    .we-tool-btn {
      display: grid;
      place-items: center;
      width: 36px;
      height: 36px;
      border: 1px solid transparent;
      border-radius: 10px;
      background: transparent;
      color: var(--psy-muted);
      cursor: pointer;
      transition: all var(--psy-motion-fast);
    }
    .we-tool-btn:hover { background: rgba(79,124,172,.08); color: var(--psy-blue-deep); }
    .we-tool-btn.active {
      background: rgba(79,124,172,.14);
      border-color: rgba(79,124,172,.28);
      color: var(--psy-blue-deep);
    }
    .we-tool-btn:disabled { opacity: .35; pointer-events: none; }
    .we-tool-btn mat-icon { font-size: 20px; width: 20px; height: 20px; }
    .we-divider {
      display: block;
      width: 1px;
      height: 24px;
      background: var(--psy-border);
      margin: 0 4px;
    }
    .we-zoom-label {
      font-family: 'JetBrains Mono', monospace;
      font-size: .76rem;
      font-weight: 700;
      color: var(--psy-muted);
      min-width: 42px;
      text-align: center;
    }
    .we-status {
      display: flex;
      gap: 8px;
      align-items: center;
      width: 100%;
      padding-top: 4px;
    }
    .we-badge {
      display: inline-flex;
      align-items: center;
      padding: 2px 10px;
      border-radius: 999px;
      font-size: .72rem;
      font-weight: 800;
    }
    .we-badge--dirty { background: rgba(198,168,80,.16); color: #7a6320; }
    .we-badge--saving { background: rgba(79,124,172,.12); color: var(--psy-blue-deep); }
    .we-info {
      font-size: .76rem;
      color: var(--psy-muted);
      font-family: 'JetBrains Mono', monospace;
    }

    /* Layout */
    .we-layout {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 300px;
      gap: 14px;
      align-items: start;
    }
    .we-canvas-wrap {
      position: relative;
      border: 1px solid rgba(79,124,172,.16);
      border-radius: 16px;
      overflow: hidden;
      background: #f8fbfd;
      min-height: 400px;
    }
    .we-konva-host { width: 100%; height: 540px; }

    /* Inspector */
    .we-inspector {
      display: grid;
      gap: 14px;
      padding: 16px;
      border-radius: 18px;
      max-height: 80vh;
      overflow-y: auto;
    }
    .we-inspector h4 {
      margin: 0;
      font-family: 'Poppins', system-ui, sans-serif;
      letter-spacing: 0;
      font-size: 1.05rem;
      color: var(--psy-ink);
    }
    .we-form { display: grid; gap: 8px; }
    .we-form label {
      display: grid;
      gap: 3px;
      font-size: .82rem;
    }
    .we-form label > span { font-weight: 700; color: var(--psy-blue-deep); }
    .we-form input, .we-form select {
      padding: 6px 10px;
      border: 1px solid rgba(79,124,172,.18);
      border-radius: 8px;
      background: rgba(255,255,255,.8);
      font-size: .84rem;
      color: var(--psy-ink);
      font-family: inherit;
    }
    .we-form input:focus, .we-form select:focus {
      outline: none;
      border-color: var(--psy-blue);
      box-shadow: 0 0 0 2px var(--psy-focus);
    }
    .we-form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
    .we-check {
      display: flex !important;
      align-items: center;
      gap: 8px;
      flex-direction: row !important;
    }
    .we-check input { width: auto; }
    .we-delete-btn { color: #8b3145 !important; margin-top: 6px; }

    .we-inspector-empty {
      display: grid;
      gap: 10px;
      text-align: center;
      padding: 20px 0;
      color: var(--psy-muted);
    }
    .we-inspector-empty mat-icon {
      justify-self: center;
      font-size: 36px;
      width: 36px;
      height: 36px;
      color: rgba(79,124,172,.3);
    }
    .we-inspector-empty p { margin: 0; font-size: .84rem; line-height: 1.5; }
    .we-hint { font-size: .72rem !important; font-family: 'JetBrains Mono', monospace; }

    /* Validation */
    .we-validation {
      display: grid;
      gap: 10px;
      padding-top: 12px;
      border-top: 1px solid var(--psy-border);
    }
    .we-validation h4 {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .we-val-section { display: grid; gap: 6px; }
    .we-val-item {
      display: flex;
      gap: 8px;
      align-items: flex-start;
      font-size: .8rem;
      line-height: 1.4;
    }
    .we-val-errors .we-val-item { color: #8b3145; }
    .we-val-errors mat-icon { color: #a85064; font-size: 18px; width: 18px; height: 18px; flex-shrink: 0; }
    .we-val-warnings .we-val-item { color: #7a6320; }
    .we-val-warnings mat-icon { color: #c6a850; font-size: 18px; width: 18px; height: 18px; flex-shrink: 0; }
    .we-val-ok { margin: 0; color: #2f7c5f; font-size: .84rem; }

    /* Error bar */
    .we-error {
      display: flex;
      gap: 10px;
      align-items: center;
      padding: 12px 16px;
      border-radius: 14px;
      background: rgba(168,80,98,.08);
      border: 1px solid rgba(168,80,98,.24);
      color: #8b3145;
      font-size: .88rem;
    }

    @media (max-width: 1000px) {
      .we-layout { grid-template-columns: 1fr; }
      .we-inspector { max-height: none; }
    }
  `]
})
export class WorldEditorComponent implements OnInit, OnDestroy {
  readonly store = inject(WorldEditorStore);

  readonly caseVersionId = input.required<number>();
  readonly nodeId = input<number>();

  @ViewChild('konvaHost', { static: false }) konvaHost!: ElementRef<HTMLDivElement>;
  @ViewChild('canvasWrap', { static: false }) canvasWrap!: ElementRef<HTMLDivElement>;

  private stage: Konva.Stage | null = null;
  private mainLayer: Konva.Layer | null = null;
  private gridLayer: Konva.Layer | null = null;
  private uiLayer: Konva.Layer | null = null;

  // Drawing state for collision zone creation
  private drawingZone = false;
  private drawStart = { x: 0, y: 0 };
  private drawRect: Konva.Rect | null = null;

  // Drag state for move commands
  private dragStart: { x: number; y: number } | null = null;

  // Spawn marker
  private spawnMarker: Konva.Group | null = null;

  constructor() {
    // React to editor state changes and re-render Konva
    effect(() => {
      const state = this.store.editorState();
      if (state && this.stage) {
        this.renderWorld(state);
      }
    });

    // React to selection changes
    effect(() => {
      const key = this.store.selectedKey();
      this.highlightSelected(key);
    });
  }

  ngOnInit(): void {
    // Load happens after view init
  }

  ngAfterViewInit(): void {
    this.initKonva();
    this.store.load(this.caseVersionId(), this.nodeId());
  }

  ngOnDestroy(): void {
    this.stage?.destroy();
  }

  // ─── Keyboard shortcuts ───────────────────────────────────────────────

  @HostListener('window:keydown', ['$event'])
  onKeydown(e: KeyboardEvent): void {
    const target = e.target as HTMLElement;
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.tagName === 'SELECT') return;

    if (e.ctrlKey && e.key === 'z') { e.preventDefault(); this.store.undo(); }
    else if (e.ctrlKey && e.key === 'y') { e.preventDefault(); this.store.redo(); }
    else if (e.key === 'Delete' || e.key === 'Backspace') { this.deleteSelected(); }
    else if (e.key === 'v' || e.key === 'V') { this.store.setTool('select'); }
    else if (e.key === 'o' || e.key === 'O') { this.store.setTool('place-object'); }
    else if (e.key === 'c' || e.key === 'C') { this.store.setTool('place-collision'); }
    else if (e.key === 's' && !e.ctrlKey) { this.store.setTool('place-spawn'); }
    else if (e.key === 'h' || e.key === 'H') { this.store.setTool('pan'); }
    else if (e.key === '+' || e.key === '=') { this.zoomIn(); }
    else if (e.key === '-') { this.zoomOut(); }
    else if (e.key === '0') { this.zoomFit(); }
  }

  // ─── Zoom controls ────────────────────────────────────────────────────

  zoomIn(): void {
    this.store.setZoom(this.store.zoom() + 0.15);
    this.applyZoom();
  }

  zoomOut(): void {
    this.store.setZoom(this.store.zoom() - 0.15);
    this.applyZoom();
  }

  zoomFit(): void {
    if (!this.stage || !this.canvasWrap) return;
    const state = this.store.editorState();
    if (!state) return;
    const wrapW = this.canvasWrap.nativeElement.clientWidth;
    const fitZoom = Math.min(wrapW / state.map.width, 540 / state.map.height, 2);
    this.store.setZoom(fitZoom);
    this.applyZoom();
  }

  // ─── Inspector actions ────────────────────────────────────────────────

  updateObjectField(key: string, field: string, value: unknown): void {
    this.store.execute(new UpdateInspectorCommand(key, { [field]: value } as Partial<WorldObject>));
  }

  updateZoneLabel(key: string, label: string): void {
    const state = this.store.editorState();
    if (!state) return;
    const zone = state.collisionZones.find(z => z.key === key);
    if (!zone) return;
    this.store.execute(new ResizeZoneCommand(key,
      { x: zone.x, y: zone.y, width: zone.width, height: zone.height },
      { x: zone.x, y: zone.y, width: zone.width, height: zone.height }
    ));
    // Update label directly since ResizeZoneCommand doesn't handle labels
    const updatedState = this.store.editorState();
    if (updatedState) {
      this.store.editorState.set({
        ...updatedState,
        collisionZones: updatedState.collisionZones.map(z =>
          z.key === key ? { ...z, label } : z
        )
      });
    }
  }

  deleteSelected(): void {
    const key = this.store.selectedKey();
    if (!key) return;
    if (this.store.selectedObject()) {
      this.store.execute(new DeleteObjectCommand(key));
    } else if (this.store.selectedZone()) {
      this.store.execute(new DeleteCollisionZoneCommand(key));
    }
    this.store.select(null);
  }

  deleteSelectedZone(): void {
    const key = this.store.selectedKey();
    if (!key) return;
    this.store.execute(new DeleteCollisionZoneCommand(key));
    this.store.select(null);
  }

  // ─── Konva initialization ─────────────────────────────────────────────

  private initKonva(): void {
    if (!this.konvaHost) return;

    const container = this.konvaHost.nativeElement;
    const width = container.clientWidth || 960;
    const height = 540;

    this.stage = new Konva.Stage({
      container,
      width,
      height
    });

    this.gridLayer = new Konva.Layer();
    this.mainLayer = new Konva.Layer();
    this.uiLayer = new Konva.Layer();

    this.stage.add(this.gridLayer);
    this.stage.add(this.mainLayer);
    this.stage.add(this.uiLayer);

    // Mouse events for tools
    this.stage.on('mousedown touchstart', (e) => this.onStageMouseDown(e));
    this.stage.on('mousemove touchmove', (e) => this.onStageMouseMove(e));
    this.stage.on('mouseup touchend', () => this.onStageMouseUp());
    this.stage.on('click tap', (e) => this.onStageClick(e));

    // Wheel zoom
    this.stage.on('wheel', (e) => {
      e.evt.preventDefault();
      const delta = e.evt.deltaY > 0 ? -0.1 : 0.1;
      this.store.setZoom(this.store.zoom() + delta);
      this.applyZoom();
    });
  }

  // ─── Rendering ────────────────────────────────────────────────────────

  private renderWorld(state: { map: { width: number; height: number; spawnX: number; spawnY: number; theme: string }; objects: WorldObject[]; collisionZones: WorldCollisionZone[] }): void {
    if (!this.mainLayer || !this.gridLayer || !this.uiLayer) return;

    this.mainLayer.destroyChildren();
    this.gridLayer.destroyChildren();
    this.uiLayer.destroyChildren();

    const { map, objects, collisionZones } = state;

    // ─── Grid ───────────────────────────────────────────────────────
    this.drawGrid(map.width, map.height);

    // ─── Map bounds ─────────────────────────────────────────────────
    this.mainLayer.add(new Konva.Rect({
      x: 0, y: 0,
      width: map.width, height: map.height,
      stroke: '#4f7cac',
      strokeWidth: 2,
      fill: 'transparent',
      dash: [6, 4],
      listening: false
    }));

    // ─── Collision zones ────────────────────────────────────────────
    for (const zone of collisionZones) {
      const group = new Konva.Group({
        x: zone.x, y: zone.y,
        name: zone.key,
        draggable: this.store.activeTool() === 'select'
      });

      group.add(new Konva.Rect({
        width: zone.width,
        height: zone.height,
        fill: 'rgba(79,124,172,0.08)',
        stroke: 'rgba(79,124,172,0.4)',
        strokeWidth: 1,
        dash: [4, 3],
        cornerRadius: 3
      }));

      if (zone.label) {
        group.add(new Konva.Text({
          x: 4, y: 4,
          text: zone.label,
          fontSize: 10,
          fontFamily: 'Arial, sans-serif',
          fill: '#4f7cac',
          listening: false
        }));
      }

      group.on('click tap', (e) => {
        e.cancelBubble = true;
        this.store.select(zone.key);
      });

      this.mainLayer.add(group);
    }

    // ─── Objects ────────────────────────────────────────────────────
    for (const obj of objects) {
      const color = OBJECT_COLORS[obj.type] ?? '#4fa3a5';
      const group = new Konva.Group({
        x: obj.x, y: obj.y,
        name: obj.key,
        draggable: this.store.activeTool() === 'select',
        opacity: obj.visible ? 1 : 0.4
      });

      // Object body
      group.add(new Konva.Circle({
        radius: 18,
        fill: color,
        stroke: '#fff',
        strokeWidth: 2,
        shadowColor: color,
        shadowBlur: 8,
        shadowOpacity: 0.25
      }));

      // Short code label
      group.add(new Konva.Text({
        text: obj.shortCode || obj.key.slice(0, 3).toUpperCase(),
        fontSize: 9,
        fontFamily: 'Arial, sans-serif',
        fontStyle: 'bold',
        fill: '#fff',
        align: 'center',
        width: 30,
        x: -15,
        y: -5,
        listening: false
      }));

      // Label below
      group.add(new Konva.Text({
        text: obj.label,
        fontSize: 10,
        fontFamily: 'Arial, sans-serif',
        fill: '#24323a',
        align: 'center',
        width: 100,
        x: -50,
        y: 22,
        listening: false
      }));

      // Drag events for undo-able move
      group.on('dragstart', () => {
        this.dragStart = { x: obj.x, y: obj.y };
      });
      group.on('dragend', () => {
        if (!this.dragStart) return;
        const newX = this.snapToGrid(group.x());
        const newY = this.snapToGrid(group.y());
        group.position({ x: newX, y: newY });
        this.store.execute(new MoveObjectCommand(
          obj.key, this.dragStart.x, this.dragStart.y, newX, newY
        ));
        this.dragStart = null;
      });

      group.on('click tap', (e) => {
        e.cancelBubble = true;
        this.store.select(obj.key);
      });

      this.mainLayer.add(group);
    }

    // ─── Spawn marker ───────────────────────────────────────────────
    this.spawnMarker = new Konva.Group({
      x: map.spawnX,
      y: map.spawnY,
      listening: false
    });
    this.spawnMarker.add(new Konva.Circle({
      radius: 10,
      fill: '#2f7c5f',
      stroke: '#fff',
      strokeWidth: 2,
      opacity: 0.8
    }));
    this.spawnMarker.add(new Konva.Text({
      text: 'SP',
      fontSize: 8,
      fontStyle: 'bold',
      fill: '#fff',
      align: 'center',
      width: 16,
      x: -8,
      y: -4,
      listening: false
    }));
    this.uiLayer.add(this.spawnMarker);

    this.mainLayer.draw();
    this.uiLayer.draw();
    this.highlightSelected(this.store.selectedKey());
  }

  private drawGrid(mapWidth: number, mapHeight: number): void {
    if (!this.gridLayer) return;

    // Background
    this.gridLayer.add(new Konva.Rect({
      x: 0, y: 0,
      width: mapWidth, height: mapHeight,
      fill: '#f8fbfd',
      listening: false
    }));

    // Grid lines
    for (let x = 0; x <= mapWidth; x += GRID_SIZE) {
      this.gridLayer.add(new Konva.Line({
        points: [x, 0, x, mapHeight],
        stroke: 'rgba(79,124,172,0.06)',
        strokeWidth: x % (GRID_SIZE * 4) === 0 ? 1 : 0.5,
        listening: false
      }));
    }
    for (let y = 0; y <= mapHeight; y += GRID_SIZE) {
      this.gridLayer.add(new Konva.Line({
        points: [0, y, mapWidth, y],
        stroke: 'rgba(79,124,172,0.06)',
        strokeWidth: y % (GRID_SIZE * 4) === 0 ? 1 : 0.5,
        listening: false
      }));
    }
    this.gridLayer.draw();
  }

  // ─── Mouse handlers ───────────────────────────────────────────────────

  private onStageMouseDown(e: Konva.KonvaEventObject<MouseEvent | TouchEvent>): void {
    const tool = this.store.activeTool();
    const pos = this.getPointerPos();
    if (!pos) return;

    if (tool === 'place-collision') {
      this.drawingZone = true;
      this.drawStart = { x: this.snapToGrid(pos.x), y: this.snapToGrid(pos.y) };
      this.drawRect = new Konva.Rect({
        x: this.drawStart.x,
        y: this.drawStart.y,
        width: 0, height: 0,
        fill: 'rgba(79,124,172,0.12)',
        stroke: 'rgba(79,124,172,0.5)',
        strokeWidth: 1,
        dash: [4, 3]
      });
      this.uiLayer?.add(this.drawRect);
    }
  }

  private onStageMouseMove(_e: Konva.KonvaEventObject<MouseEvent | TouchEvent>): void {
    if (!this.drawingZone || !this.drawRect) return;
    const pos = this.getPointerPos();
    if (!pos) return;

    const x = Math.min(this.drawStart.x, pos.x);
    const y = Math.min(this.drawStart.y, pos.y);
    const w = Math.abs(pos.x - this.drawStart.x);
    const h = Math.abs(pos.y - this.drawStart.y);

    this.drawRect.setAttrs({ x, y, width: w, height: h });
    this.uiLayer?.batchDraw();
  }

  private onStageMouseUp(): void {
    if (this.drawingZone && this.drawRect) {
      const x = this.snapToGrid(this.drawRect.x());
      const y = this.snapToGrid(this.drawRect.y());
      const w = this.snapToGrid(this.drawRect.width());
      const h = this.snapToGrid(this.drawRect.height());

      if (w >= GRID_SIZE && h >= GRID_SIZE) {
        const key = this.store.generateKey('zone');
        const zone: WorldCollisionZone = {
          id: this.store.nextLocalId(),
          key,
          label: null,
          x, y,
          width: w,
          height: h
        };
        this.store.execute(new PlaceCollisionZoneCommand(zone));
      }

      this.drawRect.destroy();
      this.drawRect = null;
      this.drawingZone = false;
      this.uiLayer?.batchDraw();
    }
  }

  private onStageClick(e: Konva.KonvaEventObject<MouseEvent>): void {
    const tool = this.store.activeTool();
    const pos = this.getPointerPos();
    if (!pos) return;

    // Click on empty space → deselect
    if (e.target === this.stage) {
      this.store.select(null);
    }

    if (tool === 'place-object') {
      const x = this.snapToGrid(pos.x);
      const y = this.snapToGrid(pos.y);
      const key = this.store.generateKey('obj');
      const obj: WorldObject = {
        id: this.store.nextLocalId(),
        key,
        label: 'Nuevo objeto',
        type: 'OBJECT' as WorldObjectType,
        x, y,
        width: 48, height: 48,
        zIndex: 0,
        facing: 'down',
        colorHex: '#4FA3A5',
        icon: 'psychology',
        shortCode: 'NEW',
        collision: false,
        visible: true,
        interactionPrompt: 'Interactuar',
        interactionText: '',
        decisionOptionId: null,
        toolCode: null,
        unlockCondition: {},
        movementPattern: {},
        metadata: {}
      };
      this.store.execute(new PlaceObjectCommand(obj));
      this.store.select(key);
      this.store.setTool('select');
    }

    if (tool === 'place-spawn') {
      const x = this.snapToGrid(pos.x);
      const y = this.snapToGrid(pos.y);
      this.store.execute(new UpdateSpawnCommand(x, y));
      this.store.setTool('select');
    }
  }

  // ─── Utilities ────────────────────────────────────────────────────────

  private getPointerPos(): { x: number; y: number } | null {
    if (!this.stage) return null;
    const pointer = this.stage.getPointerPosition();
    if (!pointer) return null;
    // Account for zoom/pan
    const transform = this.mainLayer?.getAbsoluteTransform().copy().invert();
    if (transform) {
      return transform.point(pointer);
    }
    return pointer;
  }

  private snapToGrid(value: number): number {
    return Math.round(value / GRID_SIZE) * GRID_SIZE;
  }

  private applyZoom(): void {
    if (!this.stage || !this.mainLayer || !this.gridLayer || !this.uiLayer) return;
    const zoom = this.store.zoom();
    this.mainLayer.scale({ x: zoom, y: zoom });
    this.gridLayer.scale({ x: zoom, y: zoom });
    this.uiLayer.scale({ x: zoom, y: zoom });
    this.stage.batchDraw();
  }

  private highlightSelected(key: string | null): void {
    if (!this.mainLayer) return;

    // Remove previous highlights
    this.mainLayer.find('.selection-ring').forEach(n => n.destroy());

    if (!key) { this.mainLayer.batchDraw(); return; }

    const node = this.mainLayer.findOne(`[name=${key}]`);
    if (!node) { this.mainLayer.batchDraw(); return; }

    // Object selection ring
    if (this.store.selectedObject()) {
      const ring = new Konva.Circle({
        radius: 24,
        stroke: '#4f7cac',
        strokeWidth: 2,
        dash: [4, 3],
        fill: 'transparent',
        name: 'selection-ring',
        listening: false
      });
      (node as Konva.Group).add(ring);
    }
    // Zone selection ring
    else if (this.store.selectedZone()) {
      const zone = this.store.selectedZone()!;
      const ring = new Konva.Rect({
        x: -2, y: -2,
        width: zone.width + 4,
        height: zone.height + 4,
        stroke: '#4f7cac',
        strokeWidth: 2,
        dash: [4, 3],
        fill: 'transparent',
        name: 'selection-ring',
        listening: false
      });
      (node as Konva.Group).add(ring);
    }

    this.mainLayer.batchDraw();
  }
}
