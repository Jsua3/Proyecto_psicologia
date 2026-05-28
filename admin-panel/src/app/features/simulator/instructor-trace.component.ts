import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { SimulationService } from '../../core/api/simulation.service';
import { AttemptTrace, RecentAttempt, RubricEvaluationView } from '../../core/models/simulation.model';

@Component({
  selector: 'app-instructor-trace',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatProgressBarModule],
  template: `
    <section class="trace-page">
      <header class="trace-hero liquid-glass psy-game-panel">
        <div>
          <p class="psy-eyebrow">Panel docente</p>
          <h2>Trazabilidad y rubrica evaluativa</h2>
          <p>Revisa rutas, interacciones, herramientas, decisiones, bitacoras y competencias.</p>
        </div>
      </header>
      @if (loading()) {
        <mat-progress-bar mode="indeterminate" />
      }
      <div class="trace-layout">
        <aside class="attempt-list liquid-glass psy-game-panel">
          <h3>Intentos recientes</h3>
          @for (attempt of attempts(); track attempt.attemptId) {
            <button class="attempt-card psy-liquid-ripple" type="button" (click)="open(attempt)">
              <strong>{{ attempt.studentAlias }}</strong>
              <span>{{ attempt.caseTitle }}</span>
              <em>{{ attempt.status }} · {{ attempt.accumulatedScore }} pts</em>
            </button>
          }
        </aside>
        <main class="trace-detail">
          @if (trace(); as item) {
            <section class="trace-summary liquid-glass psy-game-panel">
              <div>
                <p class="psy-eyebrow">{{ item.studentAlias }}</p>
                <h3>{{ item.caseTitle }}</h3>
                <p>{{ item.status }} · Puntaje {{ item.accumulatedScore }} · Estres {{ item.stressIndex }}%</p>
              </div>
            </section>
            <section class="timeline liquid-glass psy-game-panel">
              <h3>Linea de tiempo</h3>
              @for (event of item.events; track event.occurredAt) {
                <article class="event-row">
                  <mat-icon>{{ iconFor(event.type) }}</mat-icon>
                  <div>
                    <strong>{{ event.type }}</strong>
                    <p>{{ event.detail || event.decisionText || event.nodeTitle }}</p>
                    <small>{{ event.occurredAt }}</small>
                  </div>
                </article>
              }
            </section>
            <section class="timeline liquid-glass psy-game-panel">
              <h3>Bitacoras</h3>
              @for (reflection of item.reflections; track reflection.nodeId) {
                <article class="reflection-row">
                  <strong>{{ reflection.nodeTitle }}</strong>
                  <p>{{ reflection.text }}</p>
                </article>
              }
            </section>
            @if (rubric(); as rub) {
              <section class="timeline liquid-glass psy-game-panel">
                <h3>{{ rub.rubricName }}</h3>
                @for (criterion of rub.criteria; track criterion.id) {
                  <article class="rubric-row">
                    <div>
                      <strong>{{ criterion.title }}</strong>
                      <p>{{ criterion.description }}</p>
                    </div>
                    <input type="number" min="0" [max]="criterion.maxScore" [(ngModel)]="rubricScores[criterion.id]" />
                  </article>
                }
                <textarea [(ngModel)]="rubricComment" rows="4" placeholder="Comentario docente"></textarea>
                <button class="psy-button psy-button--primary" type="button" (click)="saveRubric()">
                  <mat-icon>save</mat-icon>
                  Guardar rubrica
                </button>
              </section>
            }
          } @else {
            <section class="empty-state liquid-glass psy-game-panel">
              <mat-icon>timeline</mat-icon>
              <h3>Selecciona un intento</h3>
              <p>El panel mostrara recorrido, decisiones, herramientas y rubrica.</p>
            </section>
          }
        </main>
      </div>
    </section>
  `,
  styles: [`
    .trace-page { display: grid; gap: 18px; }
    .trace-hero, .trace-summary, .timeline, .attempt-list, .empty-state {
      border-radius: 22px;
      padding: 20px;
    }
    .trace-hero h2 {
      margin: 0;
      font-family: 'Cormorant Garamond', serif;
      font-size: clamp(2rem, 4vw, 3rem);
    }
    .trace-hero p:not(.psy-eyebrow), .trace-summary p, .event-row p, .reflection-row p, .rubric-row p, .empty-state p {
      margin: 6px 0 0;
      color: var(--psy-muted);
      line-height: 1.55;
    }
    .trace-layout { display: grid; grid-template-columns: minmax(260px, .36fr) minmax(0, 1fr); gap: 18px; align-items: start; }
    .attempt-list { display: grid; gap: 10px; }
    .attempt-list h3, .timeline h3, .trace-summary h3, .empty-state h3 { margin: 0; font-family: 'Cormorant Garamond', serif; }
    .attempt-card {
      display: grid;
      gap: 4px;
      min-height: 88px;
      padding: 12px;
      border: 1px solid var(--psy-border);
      border-radius: 16px;
      background: rgba(255,255,255,.62);
      text-align: left;
      cursor: pointer;
    }
    .attempt-card span, .attempt-card em { color: var(--psy-muted); font-style: normal; }
    .trace-detail { display: grid; gap: 18px; }
    .timeline { display: grid; gap: 12px; }
    .event-row, .reflection-row, .rubric-row {
      display: grid;
      grid-template-columns: auto minmax(0, 1fr);
      gap: 12px;
      padding: 12px;
      border: 1px solid var(--psy-border);
      border-radius: 16px;
      background: rgba(255,255,255,.62);
    }
    .event-row mat-icon { color: var(--psy-blue-deep); }
    .reflection-row, .rubric-row { grid-template-columns: 1fr auto; }
    .rubric-row input {
      width: 78px;
      min-height: 44px;
      border: 1px solid var(--psy-border);
      border-radius: 12px;
      padding: 0 10px;
      background: rgba(255,255,255,.75);
    }
    textarea {
      width: 100%;
      padding: 14px;
      border: 1px solid var(--psy-border);
      border-radius: 16px;
      background: rgba(255,255,255,.75);
      font: inherit;
    }
    .empty-state { display: grid; justify-items: center; text-align: center; }
    .empty-state mat-icon { font-size: 54px; width: 54px; height: 54px; color: var(--psy-blue-deep); }
    @media (max-width: 900px) { .trace-layout { grid-template-columns: 1fr; } }
  `]
})
export class InstructorTraceComponent implements OnInit {
  private readonly simulationService = inject(SimulationService);
  readonly attempts = signal<RecentAttempt[]>([]);
  readonly trace = signal<AttemptTrace | null>(null);
  readonly rubric = signal<RubricEvaluationView | null>(null);
  readonly loading = signal(true);
  rubricScores: Record<number, number> = {};
  rubricComment = '';

  ngOnInit() {
    this.simulationService.recentAttempts().subscribe({
      next: attempts => {
        this.attempts.set(attempts);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  open(attempt: RecentAttempt) {
    this.loading.set(true);
    this.simulationService.attemptTrace(attempt.attemptId).subscribe(trace => {
      this.trace.set(trace);
      this.simulationService.rubric(attempt.attemptId).subscribe(rubric => {
        this.rubric.set(rubric);
        this.rubricScores = Object.fromEntries(rubric.criteria.map(item => [item.id, 0]));
        this.loading.set(false);
      });
    });
  }

  saveRubric() {
    const trace = this.trace();
    const rubric = this.rubric();
    if (!trace || !rubric) return;
    const scores = rubric.criteria.map(criterion => ({
      criterionId: criterion.id,
      score: Number(this.rubricScores[criterion.id] ?? 0),
      comment: ''
    }));
    this.simulationService.saveRubric(trace.attemptId, rubric.rubricId, this.rubricComment, scores).subscribe(saved => this.rubric.set(saved));
  }

  iconFor(type: string) {
    if (type.includes('DECISION')) return 'bolt';
    if (type.includes('TOOL')) return 'construction';
    if (type.includes('WORLD')) return 'open_with';
    if (type.includes('REFLECTION')) return 'edit_note';
    return 'timeline';
  }
}
