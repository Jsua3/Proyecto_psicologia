import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { CasoService } from '../../core/api/caso.service';
import { Caso } from '../../core/models/caso.model';

@Component({
  selector: 'app-caso-list',
  standalone: true,
  imports: [
    CommonModule, RouterLink, MatCardModule, MatTableModule,
    MatButtonModule, MatIconModule, MatChipsModule, MatProgressBarModule
  ],
  template: `
    <div class="page-header">
      <h1 class="page-title">Casos psicosociales</h1>
      <a class="psy-button psy-button--primary" routerLink="/casos/nuevo">
        <mat-icon>add</mat-icon> Nuevo caso
      </a>
    </div>

    <mat-progress-bar *ngIf="loading()" mode="indeterminate"></mat-progress-bar>

    <mat-card>
      <mat-card-content>
        <table mat-table [dataSource]="casos()" class="full-width">
          <ng-container matColumnDef="titulo">
            <th mat-header-cell *matHeaderCellDef>Título</th>
            <td mat-cell *matCellDef="let c">{{ c.titulo }}</td>
          </ng-container>
          <ng-container matColumnDef="escenarios">
            <th mat-header-cell *matHeaderCellDef>Escenarios</th>
            <td mat-cell *matCellDef="let c">{{ c.escenarios?.length ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let c">
              <mat-chip [color]="c.activo ? 'primary' : 'warn'" highlighted>
                {{ c.activo ? 'ACTIVO' : 'INACTIVO' }}
              </mat-chip>
            </td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let c">
              <a mat-icon-button [routerLink]="['/casos', c.id, 'editar']">
                <mat-icon>edit</mat-icon>
              </a>
              <button mat-icon-button color="warn" (click)="eliminar(c.id)">
                <mat-icon>delete</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let row; columns: cols;"></tr>
        </table>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .page-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 24px; }
    .page-title { font-size: clamp(1.8rem, 3vw, 2.5rem); font-weight: 800; color: var(--siep-blue); margin: 0; letter-spacing: 0; }
    .full-width { width: 100%; }
    @media (max-width: 560px) {
      .page-header { display: grid; }
      .page-header .psy-button { width: 100%; }
    }
  `]
})
export class CasoListComponent implements OnInit {
  private casoService = inject(CasoService);
  casos = signal<Caso[]>([]);
  loading = signal(true);
  cols = ['titulo', 'escenarios', 'estado', 'acciones'];

  ngOnInit() {
    this.casoService.listar().subscribe({
      next: c => { this.casos.set(c); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  eliminar(id: number) {
    if (!confirm('¿Eliminar este caso?')) return;
    this.casoService.eliminar(id).subscribe(() => {
      this.casos.update(list => list.filter(c => c.id !== id));
    });
  }
}
