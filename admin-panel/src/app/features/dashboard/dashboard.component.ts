import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { ReporteService } from '../../core/api/reporte.service';
import { APP_BRAND } from '../../core/config/brand.config';
import { Dashboard } from '../../core/models/sesion.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatChipsModule, MatIconModule, MatTableModule],
  template: `
    <section class="dashboard-page psy-reveal">
      <div class="dashboard-heading">
        <div>
          <p class="psy-eyebrow">Seguimiento formativo</p>
          <h2>Bienvenido a {{ brand.shortName }}</h2>
          <p>{{ brand.fullName }}. Trazabilidad de decisiones, simulaciones activas y evaluación por rúbricas.</p>
        </div>
        @if (error()) {
          <div class="status-pill">
            <mat-icon>info</mat-icon>
            API no disponible
          </div>
        } @else {
          <a class="psy-button psy-button--primary" routerLink="/portal/simulador">
            <mat-icon>play_circle</mat-icon>
            Simulaciones activas
          </a>
        }
      </div>

      <div class="metric-grid">
        @for (metric of metrics(); track metric.label) {
          <article class="metric-card liquid-glass">
            <div class="metric-icon">
              <mat-icon>{{ metric.icon }}</mat-icon>
            </div>
            @if (loading()) {
              <div class="psy-skeleton skeleton-value"></div>
              <div class="psy-skeleton skeleton-label"></div>
            } @else {
              <strong>{{ metric.value }}</strong>
              <span>{{ metric.label }}</span>
            }
          </article>
        }
      </div>

      @if (error()) {
        <aside class="error-panel liquid-glass" role="status">
          <mat-icon>cloud_off</mat-icon>
          <div>
            <strong>No fue posible cargar datos del backend.</strong>
            <p>El diseño del portal permanece disponible; las métricas se actualizarán al conectar la API.</p>
          </div>
        </aside>
      }

      <div class="dashboard-grid">
        <article class="panel liquid-glass">
          <div class="panel-heading">
            <div>
              <p class="psy-eyebrow">Últimas sesiones</p>
              <h3>Intentos recientes</h3>
            </div>
            <span class="psy-chip">Trazabilidad</span>
          </div>

          @if (loading()) {
            <div class="table-skeleton">
              <div class="psy-skeleton"></div>
              <div class="psy-skeleton"></div>
              <div class="psy-skeleton"></div>
            </div>
          } @else if (rows().length) {
            <table mat-table [dataSource]="rows()" class="full-width">
              <ng-container matColumnDef="caso">
                <th mat-header-cell *matHeaderCellDef>Caso</th>
                <td mat-cell *matCellDef="let s">{{ s.casoTitulo }}</td>
              </ng-container>
              <ng-container matColumnDef="estudiante">
                <th mat-header-cell *matHeaderCellDef>Estudiante</th>
                <td mat-cell *matCellDef="let s">{{ s.estudiante }}</td>
              </ng-container>
              <ng-container matColumnDef="puntaje">
                <th mat-header-cell *matHeaderCellDef>Puntaje</th>
                <td mat-cell *matCellDef="let s" class="psy-mono">{{ s.puntaje }}</td>
              </ng-container>
              <ng-container matColumnDef="estado">
                <th mat-header-cell *matHeaderCellDef>Estado</th>
                <td mat-cell *matCellDef="let s">
                  <mat-chip [color]="s.completado ? 'primary' : 'warn'" highlighted>
                    {{ s.completado ? 'Completado' : 'En progreso' }}
                  </mat-chip>
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="cols"></tr>
              <tr mat-row *matRowDef="let row; columns: cols;"></tr>
            </table>
          } @else {
            <div class="empty-state">
              <mat-icon>pending_actions</mat-icon>
              <strong>Sin sesiones recientes</strong>
              <span>Los intentos aparecerán aquí cuando los estudiantes completen simulaciones.</span>
            </div>
          }
        </article>

        <aside class="panel insight-panel liquid-glass">
          <p class="psy-eyebrow">Brechas de aprendizaje</p>
          <h3>Lectura docente</h3>
          <p>
            Esta vista evolucionará hacia métricas por competencias, opciones falladas, decisiones prohibidas y mejora
            pre/post intervención.
          </p>
          <div class="insight-list">
            <span><mat-icon>psychology</mat-icon> PAP y contención</span>
            <span><mat-icon>forum</mat-icon> Comunicación ética</span>
            <span><mat-icon>shield</mat-icon> Rutas de protección</span>
          </div>
        </aside>
      </div>
    </section>
  `,
  styles: [`
    .dashboard-page {
      display: grid;
      gap: 22px;
    }
    .dashboard-heading {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 18px;
      padding: 8px 4px 0;
    }
    .dashboard-heading h2 {
      margin: 0;
      font-family: 'Poppins', system-ui, sans-serif;
      font-size: clamp(2.2rem, 4vw, 3.4rem);
      line-height: 1;
      letter-spacing: 0;
    }
    .dashboard-heading p:not(.psy-eyebrow) {
      max-width: 680px;
      margin: 10px 0 0;
      color: var(--psy-muted);
      line-height: 1.55;
    }
    .status-pill {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      min-height: 42px;
      padding: 0 14px;
      border-radius: 999px;
      color: var(--psy-teal-deep);
      background: rgba(79,163,165,.1);
      font-weight: 700;
      white-space: nowrap;
    }
    .metric-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 16px;
    }
    .metric-card {
      min-height: 150px;
      display: grid;
      align-content: space-between;
      padding: 20px;
      border-radius: 18px;
    }
    .metric-icon {
      display: grid;
      place-items: center;
      width: 46px;
      height: 46px;
      border-radius: 14px;
      color: var(--psy-blue-deep);
      background: rgba(79,124,172,.12);
    }
    .metric-card strong {
      color: var(--psy-blue-deep);
      font-size: clamp(2rem, 5vw, 3rem);
      line-height: 1;
    }
    .metric-card span {
      color: var(--psy-muted);
      font-weight: 600;
    }
    .skeleton-value {
      width: 120px;
      height: 42px;
      margin-top: 22px;
    }
    .skeleton-label {
      width: 70%;
      height: 18px;
    }
    .error-panel {
      display: flex;
      gap: 14px;
      align-items: flex-start;
      padding: 16px 18px;
      border-radius: 16px;
      color: var(--psy-ink);
    }
    .error-panel mat-icon {
      color: var(--psy-teal-deep);
    }
    .error-panel strong,
    .error-panel p {
      margin: 0;
    }
    .error-panel p {
      margin-top: 4px;
      color: var(--psy-muted);
    }
    .dashboard-grid {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 360px;
      gap: 16px;
    }
    .panel {
      padding: 22px;
      border-radius: 18px;
      overflow: hidden;
    }
    .panel-heading {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
      margin-bottom: 16px;
    }
    .panel h3 {
      margin: 0;
      font-size: 1.22rem;
    }
    .full-width {
      width: 100%;
      background: transparent;
    }
    .table-skeleton {
      display: grid;
      gap: 12px;
    }
    .table-skeleton .psy-skeleton {
      height: 44px;
    }
    .empty-state {
      min-height: 220px;
      display: grid;
      place-items: center;
      text-align: center;
      color: var(--psy-muted);
      gap: 8px;
    }
    .empty-state mat-icon {
      color: var(--psy-lavender);
      font-size: 34px;
      width: 34px;
      height: 34px;
    }
    .empty-state strong {
      color: var(--psy-ink);
      font-size: 1.1rem;
    }
    .insight-panel p:not(.psy-eyebrow) {
      color: var(--psy-muted);
      line-height: 1.62;
    }
    .insight-list {
      display: grid;
      gap: 10px;
      margin-top: 20px;
    }
    .insight-list span {
      display: flex;
      align-items: center;
      gap: 10px;
      min-height: 46px;
      padding: 10px 12px;
      border-radius: 14px;
      background: rgba(255,255,255,.52);
      font-weight: 700;
    }
    .insight-list mat-icon {
      color: var(--psy-green-deep);
    }
    @media (max-width: 980px) {
      .metric-grid,
      .dashboard-grid {
        grid-template-columns: 1fr;
      }
      .dashboard-heading {
        align-items: flex-start;
        flex-direction: column;
      }
    }
  `]
})
export class DashboardComponent implements OnInit {
  readonly brand = APP_BRAND;
  private readonly reporteService = inject(ReporteService);

  readonly dashboard = signal<Dashboard | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly cols = ['caso', 'estudiante', 'puntaje', 'estado'];

  ngOnInit() {
    this.reporteService.getDashboard().subscribe({
      next: value => {
        this.dashboard.set(value);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  metrics() {
    const data = this.dashboard();
    return [
      { icon: 'task_alt', value: data ? String(data.simulacionesCompletadas ?? data.casosCompletadosHoy) : '0', label: 'Simulaciones completadas' },
      { icon: 'monitoring', value: data ? Math.round(data.puntajePromedioSimulacion || data.puntajePromedioGlobal).toString() : '0', label: 'Puntaje promedio simulación' },
      { icon: 'psychology', value: data ? String(data.decisionesAdecuadas ?? 0) : '0', label: 'Decisiones adecuadas registradas' }
    ];
  }

  rows() {
    const data = this.dashboard();
    if (data?.intentosRecientes?.length) {
      return data.intentosRecientes.map(item => ({
        casoTitulo: item.casoTitulo,
        estudiante: item.estudiante,
        puntaje: item.puntaje,
        completado: item.estado === 'COMPLETADO' || item.estado === 'SAFE_EXITED'
      }));
    }
    return data?.ultimasSesiones?.map(s => ({
      casoTitulo: s.casoTitulo,
      estudiante: s.estudiante,
      puntaje: s.puntaje,
      completado: s.completado
    })) ?? [];
  }
}
