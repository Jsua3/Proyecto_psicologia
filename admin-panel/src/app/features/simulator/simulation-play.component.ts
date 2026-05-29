import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { SimulationService } from '../../core/api/simulation.service';
import {
  DialogueState, MapObjectState, ProgressMapState, SimulationAttemptState,
  SimulationFeedback, SimulationWorldState, ToolUseResult
} from '../../core/models/simulation.model';
import { DialoguePanelComponent } from './dialogue-panel.component';
import { GameWorldComponent } from './game-world.component';
import { JournalPanelComponent, JournalSaveState } from './journal-panel.component';
import { MinimapComponent, MinimapStage } from './minimap.component';
import { SimulationHudComponent } from './simulation-hud.component';
import { ToolInventoryComponent } from './tool-inventory.component';
import { AudioService } from './audio.service';
import {
  PROTOCOL_INFO_MESSAGE,
  RESTRICTED_AREA_BLOCK_MESSAGE,
  getDisplayLabel,
  getInteractionDescription,
  isAmbientInteraction,
  isRestrictedAreaInteraction,
} from './hospital-map.config';

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
      @if (actionError()) {
        <div class="action-toast" role="alert">{{ actionError() }}</div>
      }

      @if (showResumePrompt() && pendingActiveAttempt(); as active) {
        <section class="resume-overlay" role="dialog" aria-labelledby="resume-title">
          <article class="resume-card liquid-glass">
            <p class="psy-eyebrow">Intento en progreso</p>
            <h2 id="resume-title">Continuar simulación formativa</h2>
            <p>
              Ya tienes un intento activo en <strong>{{ active.caseTitle }}</strong>.
              Puedes retomarlo desde <strong>{{ active.currentNode.title }}</strong> o iniciar uno nuevo.
            </p>
            <div class="resume-metrics">
              <span>Puntaje: {{ active.accumulatedScore }}</span>
              <span>Estrés: {{ active.stressIndex }}%</span>
              <span>Riesgo: {{ active.metrics.victimRisk }}%</span>
            </div>
            <div class="resume-actions">
              <button class="psy-button psy-button--primary" type="button" (click)="resumeAttempt()">
                Continuar intento en progreso
              </button>
              <button class="psy-button psy-button--ghost" type="button" (click)="startNewAttempt()">
                Iniciar nuevo intento
              </button>
              <a class="psy-button psy-button--ghost" routerLink="/portal/simulador">Volver al catálogo</a>
            </div>
          </article>
        </section>
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
        <app-minimap class="minimap-layer"
          [stages]="minimapStages()"
          [currentNodeKey]="game.currentNode.key"
          [visitedNodeKeys]="visitedNodeKeys()" />
        <app-tool-inventory class="tools-layer" [tools]="world()?.tools ?? []"
          [inventory]="world()?.inventory ?? []" (select)="selectTool($event)" />

        @if (nearbyInteraction(); as nb) {
          <div class="proximity-hint"
            [class.proximity-hint--exit]="nb.type === 'EXIT'"
            [class.proximity-hint--rich]="proximityDescription(nb)"
            aria-live="polite" aria-atomic="true">
            <div class="proximity-hint__body">
              <strong>{{ proximityLabel(nb) }}</strong>
              @if (proximityDescription(nb); as desc) {
                <p>{{ desc }}</p>
              }
              <span class="proximity-hint__action"><kbd>E</kbd> Presiona E para interactuar</span>
            </div>
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

        <div class="controls-hint" aria-hidden="true">Mover: WASD/flechas · E decisión o interacción · J bitácora reflexiva · Esc salida segura</div>

        <app-dialogue-panel class="dialogue-layer" [dialogue]="dialogue()" [interaction]="selectedInteraction()"
          (close)="closeDialogue()" (execute)="executeDecision($event)" (useTool)="useTool($event)" />

        <div class="stress-vignette" [class.vignette--active]="stressVignetteLevel() > 0"
          [style.--vignette-opacity]="stressVignetteLevel()" aria-hidden="true"></div>

        <app-journal-panel #journalPanel class="journal-layer" [open]="journalOpen()"
          [disabled]="game.status !== 'IN_PROGRESS' || busy()" [message]="journalMessage()"
          [saveState]="journalSaveState()"
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
                [attr.aria-label]="proximityLabel(obj) + ': ' + proximityDescription(obj)"
                (click)="openInteraction(obj)">{{ proximityLabel(obj) }}</button>
            }
          </div>
        </section>

        @if (game.status !== 'IN_PROGRESS') {
          <section class="end-state-overlay liquid-glass"
            [class.end-state--safe]="game.status === 'SAFE_EXITED'" role="alert">
            <mat-icon>{{ game.status === 'COMPLETED' ? 'workspace_premium' : 'exit_to_app' }}</mat-icon>
            <div>
              <p class="psy-eyebrow">{{ game.status === 'COMPLETED' ? 'Cierre formativo' : 'Salida segura registrada' }}</p>
              <h3>{{ game.completionReport?.summaryMessage ?? (game.status === 'COMPLETED'
                ? 'El intento quedó cerrado para evaluación docente.'
                : 'El intento fue pausado de forma limpia, sin penalización.') }}</h3>
              @if (game.completionReport; as report) {
                <div class="report-grid">
                  <div><strong>Seguimiento formativo</strong><span>{{ report.finalScore }}</span></div>
                  <div><strong>Estrés final</strong><span>{{ report.finalStress }}%</span></div>
                  <div><strong>Confianza</strong><span>{{ report.metrics.userTrust }}%</span></div>
                  <div><strong>Riesgo</strong><span>{{ report.metrics.victimRisk }}%</span></div>
                </div>
                <ul class="report-list">
                  <li>Adecuadas: {{ report.adequateDecisions }}</li>
                  <li>Riesgosas: {{ report.riskyDecisions }}</li>
                  <li>Inadecuadas: {{ report.inadequateDecisions }}</li>
                  @if (report.prohibitedDecisions) { <li>Alertas éticas: {{ report.prohibitedDecisions }}</li> }
                </ul>
                @if (report.competencies.length) {
                  <p><strong>Competencias trabajadas:</strong> {{ report.competencies.join(' · ') }}</p>
                }
                @if (report.recommendations.length) {
                  <p><strong>Recomendaciones:</strong> {{ report.recommendations.join(' ') }}</p>
                }
              }
              @if (game.supportResources.length) {
                <ul class="support-list">
                  @for (resource of game.supportResources; track resource) {
                    <li>{{ resource }}</li>
                  }
                </ul>
              }
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
      max-width: min(520px, calc(100vw - 32px));
      padding: 10px 14px; border-radius: 12px;
      background: rgba(8,12,18,.88); border: 1px solid rgba(79,163,165,.3);
      color: #e8f0f4; pointer-events: none;
      animation: hint-rise 160ms ease both;
    }
    .proximity-hint--rich { text-align: left; }
    .proximity-hint__body { display: grid; gap: 4px; }
    .proximity-hint__body strong {
      font-size: .84rem;
      color: #9dc0e8;
      line-height: 1.3;
    }
    .proximity-hint__body p {
      margin: 0;
      font-size: .76rem;
      line-height: 1.4;
      color: rgba(232,240,244,.78);
    }
    .proximity-hint__action {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: .7rem;
      font-weight: 700;
      color: rgba(79,163,165,.85);
      margin-top: 2px;
    }
    .proximity-hint--exit { border-color: rgba(79,163,165,.6); }
    .proximity-hint--exit .proximity-hint__body strong { color: #4fa3a5; }
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
    .end-state-overlay h3 { margin: 0; font-family: 'Poppins', system-ui, sans-serif; font-size: 1.5rem; letter-spacing: 0; }
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
    .action-toast {
      position: absolute; top: 72px; left: 50%; transform: translateX(-50%); z-index: 320;
      max-width: min(92vw, 520px); padding: 10px 16px; border-radius: 12px;
      background: rgba(143,47,61,.92); color: #fff; font-weight: 700; text-align: center;
    }
    .resume-overlay {
      position: absolute; inset: 0; z-index: 400; display: grid; place-items: center;
      padding: 24px; background: rgba(8,12,18,.88);
    }
    .resume-card {
      width: min(560px, 100%); padding: clamp(24px, 4vw, 36px); color: #e8f0f4; text-align: left;
    }
    .resume-card h2 { margin: 12px 0; color: #fff; font-size: 1.6rem; }
    .resume-card p { margin: 0; color: rgba(232,240,244,.72); line-height: 1.6; }
    .resume-metrics {
      display: flex; flex-wrap: wrap; gap: 10px; margin: 18px 0;
    }
    .resume-metrics span {
      padding: 8px 12px; border-radius: 999px; background: rgba(255,255,255,.06);
      border: 1px solid rgba(255,255,255,.08); font-size: .82rem; font-weight: 700;
    }
    .resume-actions { display: grid; gap: 10px; margin-top: 20px; }
    .resume-actions .psy-button { width: 100%; }
    .report-grid {
      display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin: 14px 0;
    }
    .report-grid div {
      display: grid; gap: 4px; padding: 10px; border-radius: 12px;
      background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.08);
    }
    .report-grid strong { font-size: .72rem; color: rgba(232,240,244,.55); text-transform: uppercase; letter-spacing: .06em; }
    .report-grid span { font-size: 1.2rem; color: #4fa3a5; font-weight: 800; }
    .report-list, .support-list { text-align: left; margin: 10px auto; padding-left: 18px; color: rgba(232,240,244,.65); }
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
  readonly pendingActiveAttempt = signal<SimulationAttemptState | null>(null);
  readonly showResumePrompt = signal(false);
  readonly progressMap = signal<ProgressMapState | null>(null);
  readonly world      = signal<SimulationWorldState | null>(null);
  readonly loading    = signal(true);
  readonly busy       = signal(false);
  readonly error      = signal('');
  readonly actionError = signal('');
  readonly journalMessage      = signal('');
  readonly journalSaveState    = signal<JournalSaveState>('idle');
  readonly nearbyInteraction   = signal<MapObjectState | null>(null);
  readonly selectedInteraction = signal<MapObjectState | null>(null);
  readonly dialogue    = signal<DialogueState | null>(null);
  readonly stressPulse = signal(false);
  readonly a11yAnnouncement = signal('');
  readonly journalOpen  = signal(false);
  readonly fadeActive   = signal(false);

  readonly minimapStages = computed<MinimapStage[]>(() => {
    const map = this.progressMap();
    if (map?.nodes.length) {
      return map.nodes.map(node => ({ key: node.key, label: node.label }));
    }
    const game = this.attempt();
    return game ? [{ key: game.currentNode.key, label: game.currentNode.title.slice(0, 14) }] : [];
  });

  readonly visitedNodeKeys = computed(() => this.progressMap()?.visitedNodeKeys ?? []);

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

    this.simulationService.getActiveAttempt(id).subscribe({
      next: active => {
        if (active?.status === 'IN_PROGRESS') {
          this.pendingActiveAttempt.set(active);
          this.showResumePrompt.set(true);
          this.loading.set(false);
          return;
        }
        this.beginAttempt(id, false);
      },
      error: () => this.beginAttempt(id, false)
    });
  }

  resumeAttempt() {
    const active = this.pendingActiveAttempt();
    if (!active) return;
    this.showResumePrompt.set(false);
    this.loading.set(true);
    this.bootstrapAttempt(active);
  }

  startNewAttempt() {
    const id = Number(this.route.snapshot.paramMap.get('caseVersionId'));
    if (!id) return;
    this.showResumePrompt.set(false);
    this.loading.set(true);
    this.beginAttempt(id, true);
  }

  private beginAttempt(caseVersionId: number, forceNew: boolean) {
    this.simulationService.startAttempt(caseVersionId, forceNew).subscribe({
      next: attempt => this.bootstrapAttempt(attempt),
      error: () => {
        this.error.set('No pudimos iniciar la simulación. Revisa tus permisos.');
        this.loading.set(false);
      }
    });
  }

  private bootstrapAttempt(attempt: SimulationAttemptState) {
    this.attempt.set(attempt);
    this.persistAttemptToken(attempt);
    this.loadProgressMap(attempt);
    this.loadWorld(attempt);
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

    if (isAmbientInteraction(interaction.key)) {
      this.showAmbientDialogue(interaction);
      return;
    }

    if (isRestrictedAreaInteraction(interaction.key)) {
      this.showRestrictedAreaBlock(interaction);
      return;
    }

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
      error: () => { this.showActionError('No pudimos abrir la interacción.'); this.busy.set(false); }
    });
  }

  proximityLabel(obj: MapObjectState): string {
    if (obj.type === 'EXIT') return `${getDisplayLabel(obj)} →`;
    return getDisplayLabel(obj);
  }

  proximityDescription(obj: MapObjectState): string {
    return getInteractionDescription(obj);
  }

  private showRestrictedAreaBlock(interaction: MapObjectState) {
    this.selectedInteraction.set(interaction);
    this.gameWorld?.focus(interaction.key);
    this.dialogue.set({
      key: 'hospital-restricted-area',
      speakerName: 'Protocolo clínico',
      portraitKey: 'block',
      emotion: 'concerned',
      lines: [{
        order: 1,
        speakerName: 'Protocolo clínico',
        text: RESTRICTED_AREA_BLOCK_MESSAGE,
        emotion: 'concerned',
      }],
      choices: [],
    });
  }

  private showAmbientDialogue(interaction: MapObjectState) {
    this.selectedInteraction.set(interaction);
    this.dialogue.set({
      key: interaction.key,
      speakerName: interaction.label,
      portraitKey: 'info',
      emotion: 'neutral',
      lines: [{
        order: 1,
        speakerName: interaction.label,
        text: interaction.key === 'ambient:protocolo-noticia-dificil'
          ? PROTOCOL_INFO_MESSAGE
          : getInteractionDescription(interaction),
        emotion: 'neutral',
      }],
      choices: [],
    });
  }

  private showActionError(message: string) {
    this.actionError.set(message);
    window.setTimeout(() => this.actionError.set(''), 4500);
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
          this.persistAttemptToken(updated);
          this.selectedInteraction.set(null);
          this.nearbyInteraction.set(null);
          this.journalPanel?.clear();
          this.loadProgressMap(updated);
          this.loadWorld(updated);
          if (updated.feedback) {
            window.setTimeout(() => this.dialogue.set(this.buildSupervisionDialogue(updated.feedback!)), 400);
          }
        },
        error: () => { this.showActionError('No pudimos ejecutar la intervención.'); this.busy.set(false); this.fadeActive.set(false); }
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
      error: () => { this.showActionError('No pudimos usar la herramienta.'); this.busy.set(false); }
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
    this.journalSaveState.set('saving');
    this.simulationService.saveReflection(game.attemptId, game.attemptToken, game.currentNode.id, text.trim()).subscribe({
      next: () => {
        this.journalMessage.set('Bitácora guardada y cifrada.');
        this.journalSaveState.set('saved');
        this.busy.set(false);
      },
      error: () => {
        this.journalMessage.set('No pudimos guardar la bitácora.');
        this.journalSaveState.set('error');
        this.busy.set(false);
      }
    });
  }

  safeExit() {
    const game = this.attempt();
    if (!game || this.busy()) return;
    this.busy.set(true);
    this.simulationService.safeExit(game.attemptId, game.attemptToken, 'Salida segura solicitada').subscribe({
      next: updated => { this.attempt.set(updated); this.selectedInteraction.set(null); this.dialogue.set(null); this.loadWorld(updated); },
      error: () => { this.showActionError('No pudimos registrar la salida segura.'); this.busy.set(false); }
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
    lines.push({
      order: lines.length + 1,
      speakerName: '',
      text: `Puntaje ${feedback.scoreDelta >= 0 ? '+' : ''}${feedback.scoreDelta} · Estrés ${feedback.stressDelta >= 0 ? '+' : ''}${feedback.stressDelta}% · Confianza ${feedback.trustDelta >= 0 ? '+' : ''}${feedback.trustDelta} · Riesgo ${feedback.victimRiskDelta >= 0 ? '+' : ''}${feedback.victimRiskDelta}`,
      emotion: 'neutral'
    });
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

  private loadProgressMap(attempt: SimulationAttemptState) {
    this.simulationService.getProgressMap(attempt.attemptId, attempt.attemptToken).subscribe({
      next: map => this.progressMap.set(map),
      error: () => this.progressMap.set(null)
    });
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

  private persistAttemptToken(attempt: SimulationAttemptState) {
    sessionStorage.setItem(`siep_attempt_${attempt.caseVersionId}`, JSON.stringify({
      attemptId: attempt.attemptId,
      attemptToken: attempt.attemptToken
    }));
  }

  private persistPosition() {
    const game = this.attempt(), world = this.world();
    if (!game || !world || !this.lastPosition || game.status !== 'IN_PROGRESS') return;
    this.simulationService.updateWorldState(game.attemptId, game.attemptToken,
      this.lastPosition.x, this.lastPosition.y, world.map.key)
      .subscribe({ next: u => this.world.set(u), error: () => undefined });
  }
}
