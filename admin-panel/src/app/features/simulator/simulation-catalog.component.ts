import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { SimulationService } from '../../core/api/simulation.service';
import { SimulationCaseSummary } from '../../core/models/simulation.model';

@Component({
  selector: 'app-simulation-catalog',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressBarModule],
  template: `
    <section class="sim-catalog">
      <header class="catalog-hero liquid-glass">
        <div>
          <p class="psy-eyebrow">Simulador gamificado</p>
          <h2>Casos publicados para práctica deliberada.</h2>
          <p>
            Inicia un intento, toma decisiones clínicas y procedimentales, registra tu bitácora y recibe
            retroalimentación inmediata sobre impacto, riesgo y ruta ética.
          </p>
        </div>
        <div class="hero-mark" aria-hidden="true">
          <mat-icon>account_tree</mat-icon>
        </div>
      </header>

      @if (loading()) {
        <mat-progress-bar mode="indeterminate"></mat-progress-bar>
      }

      @if (error()) {
        <div class="state-card error-state" role="alert">
          <mat-icon>error</mat-icon>
          <span>{{ error() }}</span>
        </div>
      }

      <div class="case-grid">
        @for (item of cases(); track item.caseVersionId) {
          <article class="case-card liquid-glass">
            <div class="case-topline">
              <span class="psy-chip">{{ item.code }}</span>
              <span class="psy-mono">v{{ item.semanticVersion }}</span>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
            <div class="case-meta">
              <span><mat-icon>route</mat-icon>{{ item.nodeCount }} escenas</span>
              <span><mat-icon>verified</mat-icon>{{ item.status }}</span>
            </div>
            <button class="psy-button psy-button--primary" type="button" (click)="start(item)">
              <mat-icon>play_arrow</mat-icon>
              Iniciar simulación
            </button>
            <button class="psy-button psy-button--glass" type="button" (click)="openEditor(item)">
              <mat-icon>edit_note</mat-icon>
              Editor visual
            </button>
          </article>
        }
      </div>

      @if (!loading() && !cases().length && !error()) {
        <div class="state-card">
          <mat-icon>inventory_2</mat-icon>
          <span>No hay casos publicados disponibles.</span>
        </div>
      }
    </section>
  `,
  styles: [`
    .sim-catalog {
      display: grid;
      gap: 24px;
    }
    .catalog-hero {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 24px;
      padding: clamp(24px, 4vw, 42px);
      border-radius: 22px;
    }
    .catalog-hero h2 {
      max-width: 820px;
      margin: 0;
      font-family: 'Cormorant Garamond', serif;
      font-size: clamp(2rem, 4vw, 3.2rem);
      line-height: 1.02;
      letter-spacing: 0;
    }
    .catalog-hero p:not(.psy-eyebrow) {
      max-width: 760px;
      margin: 14px 0 0;
      color: var(--psy-muted);
      line-height: 1.7;
    }
    .hero-mark {
      display: grid;
      place-items: center;
      flex: 0 0 auto;
      width: 92px;
      height: 92px;
      border-radius: 24px;
      background: rgba(79,124,172,.12);
      color: var(--psy-blue-deep);
    }
    .hero-mark mat-icon {
      font-size: 46px;
      width: 46px;
      height: 46px;
    }
    .case-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 18px;
    }
    .case-card {
      display: grid;
      gap: 16px;
      padding: 22px;
      border-radius: 18px;
    }
    .case-topline,
    .case-meta {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
    }
    .case-card h3 {
      margin: 0;
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.85rem;
      line-height: 1.05;
    }
    .case-card p {
      margin: 0;
      color: var(--psy-muted);
      line-height: 1.58;
    }
    .case-meta span {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      color: var(--psy-muted);
      font-weight: 700;
      font-size: .9rem;
    }
    .case-meta mat-icon {
      color: var(--psy-teal-deep);
      font-size: 20px;
      width: 20px;
      height: 20px;
    }
    .state-card {
      display: flex;
      align-items: center;
      gap: 12px;
      min-height: 72px;
      padding: 18px;
      border: 1px solid var(--psy-border);
      border-radius: 16px;
      background: rgba(255,255,255,.72);
      color: var(--psy-muted);
      font-weight: 700;
    }
    .error-state {
      color: #8F2F3D;
      border-color: rgba(143,47,61,.22);
      background: rgba(143,47,61,.08);
    }
    @media (max-width: 620px) {
      .catalog-hero {
        display: grid;
      }
      .hero-mark {
        width: 64px;
        height: 64px;
        border-radius: 18px;
      }
    }
  `]
})
export class SimulationCatalogComponent implements OnInit {
  private readonly simulationService = inject(SimulationService);
  private readonly router = inject(Router);

  readonly cases = signal<SimulationCaseSummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit() {
    this.simulationService.listCases().subscribe({
      next: cases => {
        this.cases.set(cases);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No pudimos cargar los casos publicados.');
        this.loading.set(false);
      }
    });
  }

  start(item: SimulationCaseSummary) {
    this.router.navigate(['/portal/simulador', item.caseVersionId]);
  }

  openEditor(item: SimulationCaseSummary) {
    this.router.navigate(['/portal/casos', item.caseVersionId, 'editor']);
  }
}
