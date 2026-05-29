import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ReporteService } from '../../core/api/reporte.service';
import { ReporteGrupo } from '../../core/models/sesion.model';

@Component({
  selector: 'app-reporte-grupo',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatTableModule, MatChipsModule, MatProgressBarModule
  ],
  template: `
    <section class="report-page psy-reveal">
    <header class="report-heading">
      <p class="psy-eyebrow">Reportes académicos</p>
      <h1 class="page-title">Reportes por grupo</h1>
      <p>Consulta indicadores de seguimiento, desempeño y trazabilidad por cohorte y caso.</p>
    </header>

    <mat-card class="filter-card">
      <mat-card-content>
        <form [formGroup]="form" (ngSubmit)="generar()" class="filter-form">
          <mat-form-field appearance="outline">
            <mat-label>ID del grupo</mat-label>
            <input matInput type="number" formControlName="grupoId">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>ID del caso (legacy)</mat-label>
            <input matInput type="number" formControlName="casoId">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>ID versión simulación</mat-label>
            <input matInput type="number" formControlName="caseVersionId">
          </mat-form-field>
          <button class="psy-button psy-button--primary" type="submit" [disabled]="form.invalid || loading() || !hasReportFilter()">
            Generar reporte
          </button>
          <button class="psy-button psy-button--ghost" type="button" (click)="exportar()" *ngIf="reporte()">
            Exportar CSV
          </button>
        </form>
      </mat-card-content>
    </mat-card>

    <mat-progress-bar *ngIf="loading()" mode="indeterminate"></mat-progress-bar>

    <ng-container *ngIf="reporte()">
      @if (reporte()!.simulacion; as sim) {
        <div class="stats-grid">
          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-value">{{ sim.totalIntentos }}</div>
              <div class="stat-label">Intentos simulación</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-value">{{ sim.intentosCompletados }}</div>
              <div class="stat-label">Completados</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-value">{{ sim.intentosEnProgreso }}</div>
              <div class="stat-label">En progreso</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-value">{{ sim.puntajePromedio | number:'1.0-1' }}</div>
              <div class="stat-label">Puntaje profesional prom.</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-value">{{ sim.decisionesAdecuadas }}</div>
              <div class="stat-label">Decisiones adecuadas</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-value">{{ sim.bitacorasRegistradas }}</div>
              <div class="stat-label">Bitácoras registradas</div>
            </mat-card-content>
          </mat-card>
        </div>

        <mat-card>
          <mat-card-header><mat-card-title>Simulación por estudiante</mat-card-title></mat-card-header>
          <mat-card-content>
            <table mat-table [dataSource]="sim.estudiantes" class="full-width">
              <ng-container matColumnDef="nombre">
                <th mat-header-cell *matHeaderCellDef>Estudiante</th>
                <td mat-cell *matCellDef="let e">{{ e.nombre }}</td>
              </ng-container>
              <ng-container matColumnDef="intentos">
                <th mat-header-cell *matHeaderCellDef>Intentos</th>
                <td mat-cell *matCellDef="let e">{{ e.totalIntentos }}</td>
              </ng-container>
              <ng-container matColumnDef="puntaje">
                <th mat-header-cell *matHeaderCellDef>Puntaje prom.</th>
                <td mat-cell *matCellDef="let e">{{ e.puntajePromedio | number:'1.0-1' }}</td>
              </ng-container>
              <ng-container matColumnDef="adecuadas">
                <th mat-header-cell *matHeaderCellDef>Adecuadas</th>
                <td mat-cell *matCellDef="let e">{{ e.decisionesAdecuadas }}</td>
              </ng-container>
              <ng-container matColumnDef="bitacoras">
                <th mat-header-cell *matHeaderCellDef>Bitácoras</th>
                <td mat-cell *matCellDef="let e">{{ e.bitacorasRegistradas }}</td>
              </ng-container>
              <ng-container matColumnDef="estado">
                <th mat-header-cell *matHeaderCellDef>Estado</th>
                <td mat-cell *matCellDef="let e">
                  <mat-chip highlighted>{{ e.estado }}</mat-chip>
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="simCols"></tr>
              <tr mat-row *matRowDef="let row; columns: simCols;"></tr>
            </table>
          </mat-card-content>
        </mat-card>
      }

      <!-- Métricas legacy -->
      <div class="stats-grid" *ngIf="reporte()!.totalSesiones > 0">
        <mat-card class="stat-card">
          <mat-card-content>
            <div class="stat-value">{{ reporte()!.totalSesiones }}</div>
            <div class="stat-label">Sesiones completadas</div>
          </mat-card-content>
        </mat-card>
        <mat-card class="stat-card">
          <mat-card-content>
            <div class="stat-value">{{ reporte()!.puntajePromedio | number:'1.0-1' }}</div>
            <div class="stat-label">Puntaje promedio</div>
          </mat-card-content>
        </mat-card>
        <mat-card class="stat-card">
          <mat-card-content>
            <div class="stat-value">{{ reporte()!.tasaAciertos | number:'1.0-1' }}%</div>
            <div class="stat-label">Tasa de aciertos</div>
          </mat-card-content>
        </mat-card>
        <mat-card class="stat-card">
          <mat-card-content>
            <div class="stat-value">{{ reporte()!.tiempoPromedioMs | number:'1.0-0' }} ms</div>
            <div class="stat-label">Tiempo promedio por respuesta</div>
          </mat-card-content>
        </mat-card>
      </div>

      <!-- Tabla legacy por estudiante -->
      <mat-card *ngIf="reporte()!.estudiantes.length">
        <mat-card-header><mat-card-title>Resultados por estudiante</mat-card-title></mat-card-header>
        <mat-card-content>
          <table mat-table [dataSource]="reporte()!.estudiantes" class="full-width">
            <ng-container matColumnDef="nombre">
              <th mat-header-cell *matHeaderCellDef>Estudiante</th>
              <td mat-cell *matCellDef="let e">{{ e.nombre }}</td>
            </ng-container>
            <ng-container matColumnDef="puntaje">
              <th mat-header-cell *matHeaderCellDef>Puntaje</th>
              <td mat-cell *matCellDef="let e">{{ e.puntaje }}</td>
            </ng-container>
            <ng-container matColumnDef="aciertos">
              <th mat-header-cell *matHeaderCellDef>% Aciertos</th>
              <td mat-cell *matCellDef="let e">{{ e.porcentajeAciertos | number:'1.0-1' }}%</td>
            </ng-container>
            <ng-container matColumnDef="tiempo">
              <th mat-header-cell *matHeaderCellDef>Tiempo prom. (ms)</th>
              <td mat-cell *matCellDef="let e">{{ e.tiempoPromedioMs | number:'1.0-0' }}</td>
            </ng-container>
            <ng-container matColumnDef="estado">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let e">
                <mat-chip [color]="e.estado === 'COMPLETADO' ? 'primary' : 'warn'" highlighted>
                  {{ e.estado }}
                </mat-chip>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let row; columns: cols;"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    </ng-container>
    </section>
  `,
  styles: [`
    .report-page { display: grid; gap: 22px; }
    .report-heading { display: grid; gap: 8px; }
    .report-heading p:not(.psy-eyebrow) { margin: 0; color: var(--psy-muted); line-height: 1.55; }
    .page-title {
      font-size: clamp(2rem, 4vw, 3rem);
      font-weight: 800;
      color: var(--siep-blue);
      margin: 0;
      letter-spacing: 0;
    }
    .filter-card { margin-bottom: 0; background: var(--siep-surface); }
    .filter-form { display: flex; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
    .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 0; }
    .stat-card { border-left: 4px solid var(--siep-blue); }
    .stat-value { font-size: 28px; font-weight: 800; color: var(--siep-blue); }
    .stat-label { font-size: .82rem; color: var(--siep-muted); margin-top: 4px; font-weight: 600; }
    .full-width { width: 100%; }
    @media (max-width: 980px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
    @media (max-width: 560px) {
      .stats-grid { grid-template-columns: 1fr; }
      .filter-form .psy-button { width: 100%; }
    }
  `]
})
export class ReporteGrupoComponent {
  private reporteService = inject(ReporteService);
  private fb = inject(FormBuilder);

  form = this.fb.group({
    grupoId: [null as number | null, Validators.required],
    casoId: [null as number | null],
    caseVersionId: [null as number | null]
  });

  reporte = signal<ReporteGrupo | null>(null);
  loading = signal(false);
  cols = ['nombre', 'puntaje', 'aciertos', 'tiempo', 'estado'];
  simCols = ['nombre', 'intentos', 'puntaje', 'adecuadas', 'bitacoras', 'estado'];

  hasReportFilter() {
    const { casoId, caseVersionId } = this.form.getRawValue();
    return casoId != null || caseVersionId != null;
  }

  generar() {
    const { grupoId, casoId, caseVersionId } = this.form.getRawValue();
    if (grupoId == null || !this.hasReportFilter()) return;
    this.loading.set(true);
    this.reporteService.getReporteGrupo(grupoId, casoId, caseVersionId).subscribe({
      next: r => { this.reporte.set(r); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  exportar() {
    const { grupoId, casoId, caseVersionId } = this.form.getRawValue();
    if (grupoId == null) return;
    this.reporteService.exportarCsv(grupoId, casoId, caseVersionId).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `reporte-grupo-${grupoId}.csv`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }
}
