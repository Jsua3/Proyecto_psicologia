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
    <h1 class="page-title">Reportes por Grupo</h1>

    <mat-card class="filter-card">
      <mat-card-content>
        <form [formGroup]="form" (ngSubmit)="generar()" class="filter-form">
          <mat-form-field appearance="outline">
            <mat-label>ID del grupo</mat-label>
            <input matInput type="number" formControlName="grupoId">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>ID del caso</mat-label>
            <input matInput type="number" formControlName="casoId">
          </mat-form-field>
          <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || loading()">
            Generar reporte
          </button>
          <button mat-stroked-button type="button" (click)="exportar()" *ngIf="reporte()">
            Exportar CSV
          </button>
        </form>
      </mat-card-content>
    </mat-card>

    <mat-progress-bar *ngIf="loading()" mode="indeterminate"></mat-progress-bar>

    <ng-container *ngIf="reporte()">
      <!-- Métricas globales -->
      <div class="stats-grid">
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

      <!-- Tabla por estudiante -->
      <mat-card>
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
  `,
  styles: [`
    .page-title { font-size: 24px; font-weight: 700; color: #1A2B3C; margin-bottom: 24px; }
    .filter-card { margin-bottom: 24px; }
    .filter-form { display: flex; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
    .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
    .stat-card { border-left: 4px solid #3A5A8A; }
    .stat-value { font-size: 28px; font-weight: 700; color: #3A5A8A; }
    .stat-label { font-size: 12px; color: #666; margin-top: 4px; }
    .full-width { width: 100%; }
  `]
})
export class ReporteGrupoComponent {
  private reporteService = inject(ReporteService);
  private fb = inject(FormBuilder);

  form = this.fb.group({
    grupoId: [null as number | null, Validators.required],
    casoId: [null as number | null, Validators.required]
  });

  reporte = signal<ReporteGrupo | null>(null);
  loading = signal(false);
  cols = ['nombre', 'puntaje', 'aciertos', 'tiempo', 'estado'];

  generar() {
    const { grupoId, casoId } = this.form.value;
    this.loading.set(true);
    this.reporteService.getReporteGrupo(grupoId!, casoId!).subscribe({
      next: r => { this.reporte.set(r); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  exportar() {
    const { grupoId, casoId } = this.form.value;
    this.reporteService.exportarCsv(grupoId!, casoId!).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `reporte-grupo-${grupoId}.csv`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }
}
