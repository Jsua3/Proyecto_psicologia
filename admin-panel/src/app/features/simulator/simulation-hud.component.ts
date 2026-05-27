import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { SimulationAttemptState } from '../../core/models/simulation.model';

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

        <div class="hud-score" aria-label="Puntaje profesional: {{ game.accumulatedScore }}">
          <mat-icon aria-hidden="true">star</mat-icon>
          <strong>{{ game.accumulatedScore }}</strong>
        </div>

        <div class="hud-stress"
          [class.hud-stress--pulse]="stressPulse()"
          role="meter"
          [attr.aria-valuenow]="game.stressIndex"
          aria-valuemin="0"
          aria-valuemax="100"
          [attr.aria-label]="'Estrés: ' + game.stressIndex + '%. ' + stressLabel()">
          <span class="stress-pct" [style.color]="stressColor()">{{ game.stressIndex }}%</span>
          <div class="stress-track" aria-hidden="true">
            <span [style.width.%]="game.stressIndex" [style.background]="stressMeterGradient()"></span>
          </div>
        </div>

        <div class="hud-scene">
          <mat-icon aria-hidden="true">location_on</mat-icon>
          <span>{{ game.currentNode.title }}</span>
        </div>

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
    .hud-score mat-icon { color: #f4c875; font-size: 18px; width: 18px; height: 18px; }
    .hud-score strong {
      font-family: 'JetBrains Mono', monospace;
      font-size: .9rem;
      letter-spacing: .04em;
    }

    .hud-stress {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 0 0 160px;
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
    .hud-scene mat-icon { color: #4fa3a5; font-size: 15px; width: 15px; height: 15px; flex-shrink: 0; }
    .hud-scene span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: .8rem;
      color: rgba(232,240,244,.65);
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
      background: #4fa3a5;
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

  statusLabel(status: SimulationAttemptState['status']) {
    return { IN_PROGRESS: 'En escena', COMPLETED: 'Finalizado', SAFE_EXITED: 'Pausado' }[status];
  }
}
