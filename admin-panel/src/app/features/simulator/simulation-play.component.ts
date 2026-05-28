import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { SimulationService } from '../../core/api/simulation.service';
import {
  DialogueState, MapObjectState, SimulationAttemptState,
  SimulationFeedback, SimulationWorldState, ToolUseResult
} from '../../core/models/simulation.model';
import { DialoguePanelComponent } from './dialogue-panel.component';
import { GameWorldComponent } from './game-world.component';
import { JournalPanelComponent } from './journal-panel.component';
import { MinimapComponent, MinimapStage } from './minimap.component';
import { SimulationHudComponent } from './simulation-hud.component';
import { ToolInventoryComponent } from './tool-inventory.component';
import { AudioService } from './audio.service';

@Component({
  selector: 'app-simulation-play',
  standalone: true,
  imports: [
    CommonModule, RouterLink, MatIconModule, MatProgressBarModule,
    SimulationHudComponent, GameWorldComponent, DialoguePanelComponent,
    ToolInventoryComponent, JournalPanelComponent, MinimapComponent
  ],
  template: `
    <div class="game-container" id="main-content" tabindex="-1">
      <div class="sr-only" aria-live="assertive" role="status" aria-atomic="true">{{ a11yAnnouncement() }}</div>

      @if (loading()) {
        <div class="loading-overlay"><mat-progress-bar mode="indeterminate" aria-label="Cargando simulación" /></div>
      }
      @if (error()) {
        <div class="error-overlay" role="alert">
          <mat-icon>error</mat-icon><p>{{ error() }}</p>
          <a class="psy-button psy-button--ghost" routerLink="/portal/simulador">Volver al simulador</a>
        </div>
      }

      @if (attempt(); as game) {
        @if (world(); as w) {
          <app-game-world #gameWorld class="game-layer" [world]="w"
            [nearbyInteraction]="nearbyInteraction()" [selectedInteractionKey]="selectedInteraction()?.key ?? null"
            (proximity)="nearbyInteraction.set($event)" (interact)="openInteraction($event)"
            (positionChange)="rememberPosition($event.x, $event.y)" />
        } @else {
          <div class="world-skeleton" aria-label="Cargando mapa"></div>
        }

        <app-simulation-hud class="hud-layer" [attempt]="game" [stressPulse]="stressPulse()" />
        <app-minimap class="minimap-layer" [stages]="stages" [currentNodeKey]="game.currentNode.key" />
        <app-tool-inventory class="tools-layer" [tools]="world()?.tools ?? []"
          [inventory]="world()?.inventory ?? []" (select)="selectTool($event)" />

        @if (nearbyInteraction(); as nb) {
          <div class="proximity-hint" [class.proximity-hint--exit]="nb.type === 'EXIT'" aria-live="polite" aria-atomic="true">
            <kbd>E</kbd><span>{{ nb.type === 'EXIT' ? nb.label + ' →' : nb.label }}</span>
          </div>
        }

        <button class="journal-toggle" type="button" aria-label="Abrir bitácora (J)"
          [class.journal-toggle--active]="journalOpen()" (click)="journalOpen.set(!journalOpen())">
          <mat-icon aria-hidden="true">menu_book</mat-icon>
        </button>

        @if (game.status === 'IN_PROGRESS') {
          <button class="safe-exit-btn" type="button" (click)="safeExit()" [disabled]="busy()"
            aria-label="Salida segura (Escape)">
            <mat-icon aria-hidden="true">exit_to_app</mat-icon>
          </button>
        }

        <div class="controls-hint" aria-hidden="true">WASD/↑↓←→ · E interactuar · J bitácora · Esc salida</div>

        <app-dialogue-panel class="dialogue-layer" [dialogue]="dialogue()" [interaction]="selectedInteraction()"
          (close)="closeDialogue()" (execute)="executeDecision($event)" (useTool)="useTool($event)" />

        <div class="stress-vignette" [class.vignette--active]="stressVignetteLevel() > 0"
          [style.--vignette-opacity]="stressVignetteLevel()" aria-hidden="true"></div>

        <app-journal-panel #journalPanel class="journal-layer" [open]="journalOpen()"
          [disabled]="game.status !== 'IN_PROGRESS' || busy()" [message]="journalMessage()"
          (save)="saveReflection($event)" (closeSheet)="journalOpen.set(false)" />

        <!-- Fase 7: screen-reader narrative route -->
        <section class="sr-narrative-route" aria-label="Ruta narrativa accesible">
          <h4 class="sr-only">Escena: {{ game.currentNode.title }}</h4>
          <p class="sr-only">{{ game.currentNode.narrative }}</p>
          @if (game.currentNode.warningMessage) {
            <p class="sr-only" role="alert">Advertencia: {{ game.currentNode.warningMessage }}</p>
          }
          @if (game.currentNode.sensitiveContent) {
            <p class="sr-only" role="note">Contenido sensible. Salida segura con Escape.</p>
          }
          <div class="sr-only">Puntaje: {{ game.accumulatedScore }}. Estrés: {{ game.stressIndex }}%. Estado: {{ statusLabelA11y(game.status) }}.</div>
        </section>

        <!-- Fase 7: accessible interaction list -->
        <section class="sr-only" aria-label="Lista accesible de puntos interactivos">
          <div role="list">
            @for (obj of world()?.objects ?? []; track obj.key) {
              <button role="listitem" type="button" class="sr-only"
                [attr.aria-label]="obj.label + ': ' + obj.interactionPrompt"
                (click)="openInteraction(obj)">{{ obj.label }}</button>
            }
          </div>
        </section>

        @if (game.status !== 'IN_PROGRESS') {
          <section class="end-state-overlay liquid-glass"
            [class.end-state--safe]="game.status === 'SAFE_EXITED'" role="alert">
            <mat-icon>{{ game.status === 'COMPLETED' ? 'workspace_premium' : 'exit_to_app' }}</mat-icon>
            <div>
              <p class="psy-eyebrow">{{ game.status === 'COMPLETED' ? 'Misión completada' : 'Salida segura' }}</p>
              <h3>{{ game.status === 'COMPLETED' ? 'El intento quedó cerrado para evaluación docente.' : 'El intento fue pausado de forma limpia.' }}</h3>
              <p>{{ game.status === 'COMPLETED'
                ? 'La ruta recorrida, decisiones, puntaje, estrés y bitácoras quedaron registrados.'
                : 'Puedes revisar recursos de apoyo o retomar con acompañamiento institucional.' }}</p>
            </div>
            <a class="psy-button psy-button--primary" routerLink="/portal/simulador">
              <mat-icon aria-hidden="true">arrow_back</mat-icon>Volver al simulador
            </a>
          </section>
        }

        <div class="scene-fade" [class.scene-fade--active]="fadeActive()" aria-hidden="true"></div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .game-container { position: fixed; inset: 0; overflow: hidden; background: #0a0f14; }
    /* Reserve bottom 110 px for the dialogue strip — world never extends into it */
    .game-layer { position: absolute; inset: 0 0 110px 0; z-index: 10; }
    .world-skeleton { position: absolute; inset: 0 0 110px 0; z-index: 10; background: #0e141a; }
    app-simulation-hud.hud-layer { position: absolute; top: 0; left: 0; right: 0; z-index: 50; }
    app-minimap.minimap-layer { position: absolute; top: 62px; right: 12px; z-index: 50; }
    /* HUD buttons sit just inside the top of the dialogue-reserved zone */
    app-tool-inventory.tools-layer { position: absolute; bottom: 118px; left: 12px; z-index: 50; }
    .proximity-hint {
      position: absolute; bottom: 118px; left: 50%; transform: translateX(-50%); z-index: 50;
      display: flex; align-items: center; gap: 8px; padding: 6px 14px; border-radius: 999px;
      background: rgba(8,12,18,.82); border: 1px solid rgba(79,163,165,.3);
      color: #e8f0f4; font-size: .82rem; font-weight: 700; white-space: nowrap; pointer-events: none;
      animation: hint-rise 160ms ease both;
    }
    .proximity-hint--exit { border-color: rgba(79,163,165,.6); color: #4fa3a5; }
    .proximity-hint kbd {
      padding: 2px 7px; border-radius: 5px; background: rgba(79,163,165,.18);
      border: 1px solid rgba(79,163,165,.35); font-size: .76rem;
      font-family: 'JetBrains Mono', monospace; color: #4fa3a5;
    }
    .journal-toggle {
      position: absolute; bottom: 118px; right: 12px; z-index: 50;
      width: 44px; height: 44px; border: 1px solid rgba(79,163,165,.28); border-radius: 10px;
      background: rgba(8,12,18,.76); color: rgba(79,163,165,.6); cursor: pointer;
      display: grid; place-items: center; transition: border-color 160ms, color 160ms;
    }
    .journal-toggle--active { border-color: rgba(79,163,165,.75); color: #4fa3a5; }
    .safe-exit-btn {
      position: absolute; top: 62px; right: 12px; z-index: 51;
      width: 40px; height: 40px; border: 1px solid rgba(168,80,98,.3); border-radius: 10px;
      background: rgba(8,12,18,.7); color: rgba(168,80,98,.6); cursor: pointer;
      display: grid; place-items: center; transition: border-color 160ms, color 160ms;
    }
    .safe-exit-btn:hover { border-color: rgba(168,80,98,.6); color: rgba(168,80,98,.9); }
    .safe-exit-btn:disabled { opacity: .35; cursor: not-allowed; }
    .controls-hint {
      position: absolute; bottom: 116px; left: 50%; transform: translateX(-50%); z-index: 50;
      padding: 5px 12px; border-radius: 999px; background: rgba(8,12,18,.5);
      color: rgba(232,240,244,.3); font-size: .66rem; font-weight: 700; letter-spacing: .05em;
      pointer-events: none; white-space: nowrap;
    }
    app-dialogue-panel.dialogue-layer { position: absolute; bottom: 0; left: 0; right: 0; z-index: 60; }
    .stress-vignette {
      position: fixed; inset: 0; pointer-events: none; z-index: 90; opacity: 0;
      transition: opacity 1.2s cubic-bezier(.4,0,.2,1);
      background: radial-gradient(ellipse at center, transparent 55%, rgba(120,40,55,.22) 100%);
    }
    .vignette--active { opacity: var(--vignette-opacity, 0); }
    app-journal-panel.journal-layer { position: absolute; top: 0; right: 0; bottom: 0; width: min(400px,88vw); z-index: 100; }
    .end-state-overlay {
      position: absolute; inset: 0; z-index: 150; display: flex; flex-direction: column;
      gap: 18px; align-items: center; justify-content: center; text-align: center;
      padding: 32px; background: rgba(8,12,18,.92); color: #e8f0f4;
    }
    .end-state-overlay mat-icon {
      font-size: 48px; width: 72px; height: 72px; display: grid; place-items: center;
      border-radius: 22px; background: rgba(79,163,165,.14); color: #4fa3a5;
    }
    .end-state--safe mat-icon { color: rgba(232,240,244,.6); background: rgba(232,240,244,.06); }
    .end-state-overlay h3 { margin: 0; font-family: 'Cormorant Garamond', serif; font-size: 1.5rem; }
    .end-state-overlay p  { margin: 0; color: rgba(232,240,244,.55); line-height: 1.6; max-width: 560px; }
    .scene-fade {
      position: fixed; inset: 0; z-index: 200; background: #0a0f14; opacity: 0;
      pointer-events: none; transition: opacity 320ms ease;
    }
    .scene-fade--active { opacity: 1; pointer-events: auto; }
    .loading-overlay { position: absolute; top: 0; left: 0; right: 0; z-index: 300; }
    .error-overlay {
      position: absolute; inset: 0; z-index: 300; display: flex; flex-direction: column;
      gap: 14px; align-items: center; justify-content: center;
      background: rgba(8,12,18,.9); color: #e8f0f4; padding: 24px; text-align: center;
    }
    .error-overlay mat-icon { font-size: 36px; color: rgba(168,80,98,.8); }
    .error-overlay p { margin: 0; color: rgba(232,240,244,.6); }
    @keyframes hint-rise {
      from { opacity: 0; transform: translateX(-50%) translateY(6px); }
      to   { opacity: 1; transform: translateX(-50%) translateY(0); }
    }
    @media (prefers-reduced-motion: reduce) {
      .stress-vignette, .scene-fade { transition: none; }
    }
  `]
})
export class SimulationPlayComponent implements OnInit {
  private readonly simulationService = inject(SimulationService);
  private readonly route = inject(ActivatedRoute);
  private readonly audio = inject(AudioService);

  @ViewChild('gameWorld')    private gameWorld?: GameWorldComponent;
  @ViewChild('journalPanel') private journalPanel?: JournalPanelComponent;

  readonly attempt    = signal<SimulationAttemptState | null>(null);
  readonly world      = signal<SimulationWorldState | null>(null);
  readonly loading    = signal(true);
  readonly busy       = signal(false);
  readonly error      = signal('');
  readonly journalMessage      = signal('');
  readonly nearbyInteraction   = signal<MapObjectState | null>(null);
  readonly selectedInteraction = signal<MapObjectState | null>(null);
  readonly dialogue    = signal<DialogueState | null>(null);
  readonly stressPulse = signal(false);
  readonly a11yAnnouncement = signal('');
  readonly journalOpen  = signal(false);
  readonly fadeActive   = signal(false);

  readonly stages: MinimapStage[] = [
    { key: 'urgencias-crisis',     label: 'Crisis'  },
    { key: 'ruta-proteccion',      label: 'Ruta'    },
    { key: 'informe-integral',     label: 'Informe' },
    { key: 'valoracion-comisaria', label: 'Riesgo'  },
    { key: 'proteccion-nna',       label: 'NNA'     },
    { key: 'cierre-seguimiento',   label: 'Cierre'  }
  ];

  readonly stressVignetteLevel = computed(() => {
    const s = this.attempt()?.stressIndex ?? 0;
    if (s < 40) return 0;
    return Math.min(0.45, 0.05 + ((s - 40) / 60) * 0.4);
  });

  private lastPosition: { x: number; y: number } | null = null;
  private positionSaveHandle: number | null = null;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('caseVersionId'));
    if (!id) { this.error.set('Caso no encontrado.'); this.loading.set(false); return; }
    this.simulationService.startAttempt(id).subscribe({
      next: attempt => { this.attempt.set(attempt); this.loadWorld(attempt); },
      error: () => { this.error.set('No pudimos iniciar la simulación. Revisa tus permisos.'); this.loading.set(false); }
    });
  }

  @HostListener('window:keydown', ['$event'])
  handleGlobalInteraction(event: KeyboardEvent) {
    const tag = (event.target as HTMLElement | null)?.tagName;
    const editable = (event.target as HTMLElement | null)?.isContentEditable;

    if (event.key === 'Escape') {
      if (this.journalOpen()) { event.preventDefault(); this.journalOpen.set(false); return; }
      if (this.dialogue())    { event.preventDefault(); this.closeDialogue(); return; }
      const g = this.attempt();
      if (g?.status === 'IN_PROGRESS' && !this.busy()) { event.preventDefault(); this.safeExit(); }
      return;
    }

    if ((event.key === 'j' || event.key === 'J') && tag !== 'INPUT' && tag !== 'TEXTAREA' && !editable) {
      event.preventDefault();
      this.journalOpen.set(!this.journalOpen());
      return;
    }

    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || editable) return;

    if (['e','E',' ','Enter'].includes(event.key)) {
      const nb = this.nearbyInteraction();
      if (!nb) return;
      event.preventDefault();
      this.openInteraction(nb);
    }
  }

  openInteraction(interaction: MapObjectState) {
    const game = this.attempt();
    if (!game || game.status !== 'IN_PROGRESS') return;
    this.selectedInteraction.set(interaction);
    this.gameWorld?.focus(interaction.key);
    this.busy.set(true);
    this.simulationService.openInteraction(game.attemptId, game.attemptToken, interaction.key).subscribe({
      next: result => {
        this.world.set(result.world);
        this.selectedInteraction.set(result.interaction);
        this.dialogue.set(result.dialogue ?? result.interaction.dialogue);
        this.busy.set(false);
      },
      error: () => { this.error.set('No pudimos abrir la interacción.'); this.busy.set(false); }
    });
  }

  executeDecision(decisionOptionId: number) {
    const game = this.attempt();
    if (!game || this.busy()) return;
    this.busy.set(true);
    this.dialogue.set(null);
    this.journalMessage.set('');
    this.triggerFade(() => {
      this.simulationService.chooseDecision(game.attemptId, game.attemptToken, decisionOptionId).subscribe({
        next: updated => {
          this.attempt.set(updated);
          this.selectedInteraction.set(null);
          this.nearbyInteraction.set(null);
          this.journalPanel?.clear();
          this.loadWorld(updated);
          if (updated.feedback) {
            window.setTimeout(() => this.dialogue.set(this.buildSupervisionDialogue(updated.feedback!)), 400);
          }
        },
        error: () => { this.error.set('No pudimos ejecutar la intervención.'); this.busy.set(false); this.fadeActive.set(false); }
      });
    });
  }

  useTool(toolCode: string) {
    const game = this.attempt();
    const target = this.selectedInteraction()?.key ?? null;
    if (!game || this.busy()) return;
    this.busy.set(true);
    this.simulationService.useTool(game.attemptId, game.attemptToken, toolCode, target).subscribe({
      next: (result: ToolUseResult) => {
        this.world.set(result.world);
        const cur = this.attempt();
        if (cur) this.attempt.set({ ...cur, stressIndex: Math.max(0, Math.min(100, cur.stressIndex + result.stressDelta)) });
        this.showToolFeedback(result);
        this.busy.set(false);
      },
      error: () => { this.error.set('No pudimos usar la herramienta.'); this.busy.set(false); }
    });
  }

  selectTool(toolCode: string) {
    const tool = this.world()?.tools.find(t => t.code === toolCode);
    if (!tool) return;
    this.dialogue.set({
      key: tool.code, speakerName: 'Herramienta profesional', portraitKey: tool.icon, emotion: 'neutral',
      lines: [{ order: 1, speakerName: tool.label, text: tool.description, emotion: 'neutral' }], choices: []
    });
    this.selectedInteraction.set({
      key: `tool-${tool.code}`, label: tool.label, type: 'TOOL',
      x: 0, y: 0, width: 0, height: 0, color: '#4FA3A5', icon: tool.icon,
      shortCode: tool.code.slice(0,4), collision: false,
      interactionPrompt: 'Herramienta profesional', interactionText: tool.description,
      decisionOptionId: null, toolCode: tool.code, dialogue: null
    });
  }

  closeDialogue() { this.dialogue.set(null); }

  saveReflection(text: string) {
    const game = this.attempt();
    if (!game || !text.trim() || this.busy()) return;
    this.busy.set(true);
    this.simulationService.saveReflection(game.attemptId, game.attemptToken, game.currentNode.id, text.trim()).subscribe({
      next: () => { this.journalMessage.set('Bitácora guardada y cifrada.'); this.busy.set(false); },
      error: () => { this.journalMessage.set('No pudimos guardar la bitácora.'); this.busy.set(false); }
    });
  }

  safeExit() {
    const game = this.attempt();
    if (!game || this.busy()) return;
    this.busy.set(true);
    this.simulationService.safeExit(game.attemptId, game.attemptToken, 'Salida segura solicitada').subscribe({
      next: updated => { this.attempt.set(updated); this.selectedInteraction.set(null); this.dialogue.set(null); this.loadWorld(updated); },
      error: () => { this.error.set('No pudimos registrar la salida segura.'); this.busy.set(false); }
    });
  }

  rememberPosition(x: number, y: number) {
    this.lastPosition = { x, y };
    if (this.positionSaveHandle) window.clearTimeout(this.positionSaveHandle);
    this.positionSaveHandle = window.setTimeout(() => this.persistPosition(), 550);
  }

  statusLabelA11y(status: SimulationAttemptState['status']): string {
    return { IN_PROGRESS:'En progreso', COMPLETED:'Finalizado', SAFE_EXITED:'Pausado con salida segura' }[status];
  }

  private showToolFeedback(result: ToolUseResult): void {
    this.audio.play('tool-use');
    this.dialogue.set({
      key: `tool-feedback-${result.toolCode}-${Date.now()}`,
      speakerName: result.pertinent ? '✓ Herramienta pertinente' : 'ℹ Herramienta aplicada',
      portraitKey: null, emotion: result.pertinent ? 'positive' : 'neutral',
      lines: [
        { order: 1, speakerName: '', text: result.feedbackMessage, emotion: 'neutral' },
        { order: 2, speakerName: '', text: `Estrés ${result.stressDelta >= 0 ? '+' : ''}${result.stressDelta}%`, emotion: 'neutral' }
      ],
      choices: []
    });
    this.stressPulse.set(true);
    window.setTimeout(() => this.stressPulse.set(false), 700);
    this.announce(result.feedbackMessage);
    window.setTimeout(() => {
      if (this.dialogue()?.key?.startsWith('tool-feedback-')) this.dialogue.set(null);
    }, 5000);
  }

  private buildSupervisionDialogue(feedback: SimulationFeedback): DialogueState {
    const lines: DialogueState['lines'] = [
      { order: 1, speakerName: 'Supervisión clínica', text: feedback.message, emotion: feedback.prohibitedConduct ? 'danger' : 'neutral' }
    ];
    if (feedback.prohibitionReason) {
      lines.push({ order: 2, speakerName: 'Supervisión clínica', text: feedback.prohibitionReason, emotion: 'danger' });
    }
    lines.push({ order: lines.length+1, speakerName: '', text: `Puntaje ${feedback.scoreDelta>=0?'+':''}${feedback.scoreDelta} · Estrés ${feedback.stressDelta>=0?'+':''}${feedback.stressDelta}%`, emotion: 'neutral' });
    return {
      key: `supervision-${Date.now()}`, speakerName: 'Supervisión clínica',
      portraitKey: null,
      emotion: feedback.prohibitedConduct ? 'danger' : feedback.classification === 'ADEQUATE' ? 'positive' : 'neutral',
      lines, choices: []
    };
  }

  private triggerFade(callback: () => void): void {
    this.audio.play('scene-transition');
    this.fadeActive.set(true);
    window.setTimeout(callback, 340);
  }

  private loadWorld(attempt: SimulationAttemptState) {
    this.simulationService.getWorld(attempt.attemptId, attempt.attemptToken).subscribe({
      next: world => {
        this.world.set(world);
        this.loading.set(false);
        this.busy.set(false);
        window.setTimeout(() => this.fadeActive.set(false), 80);
      },
      error: () => {
        this.error.set('No pudimos cargar el mapa del caso.');
        this.loading.set(false);
        this.busy.set(false);
        this.fadeActive.set(false);
      }
    });
  }

  private announce(message: string): void {
    this.a11yAnnouncement.set('');
    window.setTimeout(() => this.a11yAnnouncement.set(message), 50);
  }

  private persistPosition() {
    const game = this.attempt(), world = this.world();
    if (!game || !world || !this.lastPosition || game.status !== 'IN_PROGRESS') return;
    this.simulationService.updateWorldState(game.attemptId, game.attemptToken,
      this.lastPosition.x, this.lastPosition.y, world.map.key)
      .subscribe({ next: u => this.world.set(u), error: () => undefined });
  }
}
