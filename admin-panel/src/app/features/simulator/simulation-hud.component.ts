import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { SimulationAttemptState } from '../../core/models/simulation.model';
import { getSceneObjective, getSceneProgress } from './scene-objectives.config';
import { getProximityStepHint } from './risky-interaction.config';

type StressTier = 'calm' | 'moderate' | 'high' | 'critical';

@Component({
  selector: 'app-simulation-hud',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    @if (attempt(); as game) {
      <div class="hud-strip liquid-glass"
        [class.hud--stress-high]="stressTier() === 'high'"
        [class.hud--stress-critical]="stressTier() === 'critical'">

        <div class="hud-score" aria-label="Seguimiento formativo: {{ game.accumulatedScore }} puntos">
          <span class="hud-label">Seguimiento formativo</span>
          <mat-icon aria-hidden="true">fact_check</mat-icon>
          <strong>{{ game.accumulatedScore }}</strong>
        </div>

        <div class="hud-stress"
          [class.hud-stress--pulse]="stressPulse()"
          role="meter"
          [attr.aria-valuenow]="game.stressIndex"
          aria-valuemin="0"
          aria-valuemax="100"
          [attr.aria-label]="'Estado de estrés del caso: ' + game.stressIndex + '%. ' + stressLabel()">
          <span class="hud-label">Estado del caso</span>
          <span class="stress-pct" [style.color]="stressColor()">{{ game.stressIndex }}%</span>
          <div class="stress-track" aria-hidden="true">
            <span [style.width.%]="game.stressIndex" [style.background]="stressMeterGradient()"></span>
          </div>
        </div>

        <div class="hud-scene">
          <mat-icon aria-hidden="true">location_on</mat-icon>
          <span>{{ game.currentNode.title }}</span>
        </div>

        @if (sceneProgress(); as progress) {
          <div class="hud-step" aria-label="{{ progress.stepLabel }}">
            <mat-icon aria-hidden="true">timeline</mat-icon>
            <span>{{ progress.stepLabel }}@if (progress.step > 0) { ({{ progress.step }}/{{ progress.total }}) }</span>
          </div>
        }

        @if (sceneObjective(); as objective) {
          <div class="hud-objective" role="status" aria-label="Objetivo actual: {{ objective }}">
            <mat-icon aria-hidden="true">flag</mat-icon>
            <span><strong>Objetivo:</strong> {{ objective }}</span>
          </div>
        }

        <div class="hud-status" [class.hud-status--live]="game.status === 'IN_PROGRESS'">
          <span class="status-dot" aria-hidden="true"></span>
          <span>{{ statusLabel(game.status) }}</span>
        </div>
      </div>
    }
  `,
  styles: [`
    .hud-strip {
      display: flex;
      align-items: center;
      gap: 14px;
      height: 52px;
      padding: 0 14px;
      border-radius: 0 0 16px 16px;
      background: rgba(8,12,18,.84);
      backdrop-filter: blur(18px) saturate(120%);
      border: 1px solid rgba(79,163,165,.18);
      border-top: none;
      color: rgba(232,240,244,.9);
      transition: border-color var(--psy-motion-ui);
    }
    .hud--stress-high  { border-color: rgba(212,160,80,.4); }
    .hud--stress-critical { border-color: rgba(168,80,98,.5); }

    .hud-score {
      display: flex;
      align-items: center;
      gap: 5px;
      flex-shrink: 0;
    }
    .hud-label {
      color: rgba(232,240,244,.52);
      font-size: .64rem;
      font-weight: 800;
      letter-spacing: .08em;
      text-transform: uppercase;
    }
    .hud-score mat-icon { color: var(--siep-blue-soft); font-size: 18px; width: 18px; height: 18px; }
    .hud-score strong {
      font-family: 'JetBrains Mono', monospace;
      font-size: .9rem;
      letter-spacing: .04em;
    }

    .hud-stress {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 0 0 260px;
    }
    .hud-stress--pulse { animation: stress-pulse .6s ease-out; }
    .stress-pct {
      font-family: 'JetBrains Mono', monospace;
      font-size: .78rem;
      min-width: 38px;
      transition: color var(--psy-motion-ui);
    }
    .stress-track {
      flex: 1;
      height: 5px;
      border-radius: 999px;
      background: rgba(255,255,255,.1);
      overflow: hidden;
    }
    .stress-track span {
      display: block;
      height: 100%;
      border-radius: inherit;
      transition: width .5s cubic-bezier(.4,0,.2,1), background .5s ease;
    }

    .hud-scene {
      display: flex;
      align-items: center;
      gap: 5px;
      flex: 1;
      overflow: hidden;
    }
    .hud-scene mat-icon { color: var(--siep-blue-soft); font-size: 15px; width: 15px; height: 15px; flex-shrink: 0; }
    .hud-scene span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: .8rem;
      color: rgba(232,240,244,.65);
    }

    .hud-step {
      display: flex;
      align-items: center;
      gap: 5px;
      flex-shrink: 0;
      padding: 4px 8px;
      border-radius: 8px;
      background: rgba(79,122,172,.1);
      border: 1px solid rgba(79,122,172,.2);
    }
    .hud-step mat-icon {
      color: rgba(157,192,232,.85);
      font-size: 14px;
      width: 14px;
      height: 14px;
    }
    .hud-step span {
      font-size: .68rem;
      font-weight: 700;
      color: rgba(157,192,232,.85);
      white-space: nowrap;
    }

    .hud-objective {
      display: flex;
      align-items: center;
      gap: 5px;
      flex: 1.2;
      min-width: 0;
      padding: 4px 10px;
      border-radius: 8px;
      background: rgba(79,163,165,.08);
      border: 1px solid rgba(79,163,165,.18);
    }
    .hud-objective mat-icon {
      color: var(--siep-blue-soft);
      font-size: 14px;
      width: 14px;
      height: 14px;
      flex-shrink: 0;
    }
    .hud-objective span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: .72rem;
      color: rgba(232,240,244,.78);
    }
    .hud-objective strong {
      color: rgba(157,192,232,.9);
      font-weight: 800;
      margin-right: 4px;
    }

    .hud-status {
      display: flex;
      align-items: center;
      gap: 5px;
      flex-shrink: 0;
      font-size: .7rem;
      color: rgba(232,240,244,.4);
      white-space: nowrap;
    }
    .status-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: rgba(255,255,255,.25);
    }
    .hud-status--live .status-dot {
      background: var(--siep-blue-soft);
      animation: dot-blink 2s ease-in-out infinite;
    }

    @keyframes stress-pulse {
      0%   { box-shadow: 0 0 0 0 rgba(212,160,80,.4); }
      70%  { box-shadow: 0 0 0 6px rgba(212,160,80,0); }
      100% { box-shadow: none; }
    }
    @keyframes dot-blink {
      0%, 100% { opacity: 1; }
      50%       { opacity: .35; }
    }

    @media (max-width: 640px) {
      .hud-scene { display: none; }
      .hud-step span { white-space: normal; line-height: 1.2; max-width: 120px; }
      .hud-objective span { white-space: normal; line-height: 1.25; }
      .hud-label { display: none; }
      .hud-stress { flex: 0 0 120px; }
    }
    @media (prefers-reduced-motion: reduce) {
      .hud-stress--pulse { animation: none; }
      .stress-track span { transition: none; }
      .status-dot { animation: none !important; }
    }
  `]
})
export class SimulationHudComponent {
  readonly attempt = input<SimulationAttemptState | null>(null);
  readonly stressPulse = input(false);
  readonly nearbyInteractionKey = input<string | null>(null);

  readonly stressTier = computed<StressTier>(() => {
    const s = this.attempt()?.stressIndex ?? 0;
    if (s >= 75) return 'critical';
    if (s >= 50) return 'high';
    if (s >= 25) return 'moderate';
    return 'calm';
  });

  readonly stressColor = computed(() => ({
    calm:     'var(--psy-teal-deep, #2a7a6e)',
    moderate: '#7a8a3e',
    high:     '#b07830',
    critical: '#8b3145'
  })[this.stressTier()]);

  readonly stressMeterGradient = computed(() => ({
    calm:     'linear-gradient(90deg, #8cbfa6, #4fa3a5)',
    moderate: 'linear-gradient(90deg, #8cbfa6, #c4b55a)',
    high:     'linear-gradient(90deg, #c4b55a, #d4a050)',
    critical: 'linear-gradient(90deg, #d4a050, #a85062)'
  })[this.stressTier()]);

  readonly stressLabel = computed(() => ({
    calm:     'Situación estable',
    moderate: 'Tensión moderada',
    high:     'Estrés elevado — considere herramientas de contención',
    critical: 'Nivel crítico — priorice seguridad y autocuidado'
  })[this.stressTier()]);

  readonly sceneObjective = computed(() => {
    const nodeKey = this.attempt()?.currentNode.key;
    return getSceneObjective(nodeKey);
  });

  readonly sceneProgress = computed(() => {
    const proximityLabel = getProximityStepHint(this.nearbyInteractionKey());
    if (proximityLabel) {
      return { stepLabel: proximityLabel, step: 0, total: 6 };
    }
    const nodeKey = this.attempt()?.currentNode.key;
    return getSceneProgress(nodeKey);
  });

  statusLabel(status: SimulationAttemptState['status']) {
    return { IN_PROGRESS: 'En seguimiento', COMPLETED: 'Finalizado', SAFE_EXITED: 'Salida segura' }[status];
  }
}
