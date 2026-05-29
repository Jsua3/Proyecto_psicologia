import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { GrupoService, Grupo } from '../../core/api/grupo.service';

@Component({
  selector: 'app-grupo-list',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatTableModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatIconModule, MatDialogModule
  ],
  template: `
    <div class="page-header">
      <h1 class="page-title">Grupos</h1>
    </div>

    <div class="layout">
      <!-- Formulario nuevo grupo -->
      <mat-card class="form-card">
        <mat-card-header><mat-card-title>Nuevo grupo</mat-card-title></mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="crear()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nombre del grupo</mat-label>
              <input matInput formControlName="nombre">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Código único</mat-label>
              <input matInput formControlName="codigo">
            </mat-form-field>
            <button class="psy-button psy-button--primary" type="submit" [disabled]="form.invalid">
              Crear grupo
            </button>
          </form>
        </mat-card-content>
      </mat-card>

      <!-- Lista de grupos -->
      <mat-card class="table-card">
        <mat-card-content>
          <table mat-table [dataSource]="grupos()" class="full-width">
            <ng-container matColumnDef="nombre">
              <th mat-header-cell *matHeaderCellDef>Nombre</th>
              <td mat-cell *matCellDef="let g">{{ g.nombre }}</td>
            </ng-container>
            <ng-container matColumnDef="codigo">
              <th mat-header-cell *matHeaderCellDef>Código</th>
              <td mat-cell *matCellDef="let g"><code>{{ g.codigo }}</code></td>
            </ng-container>
            <ng-container matColumnDef="estudiantes">
              <th mat-header-cell *matHeaderCellDef>Estudiantes</th>
              <td mat-cell *matCellDef="let g">{{ g.totalEstudiantes }}</td>
            </ng-container>
            <ng-container matColumnDef="acciones">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let g">
                <button mat-stroked-button (click)="agregarEstudiante(g.id)">
                  <mat-icon>person_add</mat-icon> Agregar
                </button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let row; columns: cols;"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    </div>

    <!-- Input agregar estudiante -->
    <div *ngIf="grupoSeleccionado()" class="agregar-estudiante">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Agregar estudiante al grupo</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="estudianteForm" (ngSubmit)="confirmarAgregar()">
            <mat-form-field appearance="outline">
              <mat-label>Email del estudiante</mat-label>
              <input matInput formControlName="email" type="email">
            </mat-form-field>
            <button class="psy-button psy-button--primary" type="submit" [disabled]="estudianteForm.invalid">
              Agregar
            </button>
            <button class="psy-button psy-button--ghost" type="button" (click)="grupoSeleccionado.set(null)">
              Cancelar
            </button>
          </form>
          <p *ngIf="mensajeEstudiante()" class="mensaje">{{ mensajeEstudiante() }}</p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-header { display: flex; align-items: center; margin-bottom: 24px; }
    .page-title { font-size: clamp(1.8rem, 3vw, 2.5rem); font-weight: 800; color: var(--siep-blue); margin: 0; letter-spacing: 0; }
    .layout { display: grid; grid-template-columns: 320px 1fr; gap: 16px; }
    .full-width { width: 100%; }
    .agregar-estudiante { margin-top: 16px; }
    .agregar-estudiante form { display: flex; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
    .mensaje { color: var(--psy-green-deep); font-size: .88rem; font-weight: 700; }
    @media (max-width: 920px) { .layout { grid-template-columns: 1fr; } }
    @media (max-width: 560px) {
      form .psy-button, .agregar-estudiante .psy-button { width: 100%; }
    }
  `]
})
export class GrupoListComponent implements OnInit {
  private grupoService = inject(GrupoService);
  private fb = inject(FormBuilder);

  grupos = signal<Grupo[]>([]);
  grupoSeleccionado = signal<number | null>(null);
  mensajeEstudiante = signal('');
  cols = ['nombre', 'codigo', 'estudiantes', 'acciones'];

  form = this.fb.group({ nombre: ['', Validators.required], codigo: ['', Validators.required] });
  estudianteForm = this.fb.group({ email: ['', [Validators.required, Validators.email]] });

  ngOnInit() {
    this.grupoService.listar().subscribe(g => this.grupos.set(g));
  }

  crear() {
    const { nombre, codigo } = this.form.value;
    this.grupoService.crear(nombre!, codigo!).subscribe(g => {
      this.grupos.update(list => [...list, g]);
      this.form.reset();
    });
  }

  agregarEstudiante(id: number) {
    this.grupoSeleccionado.set(id);
    this.mensajeEstudiante.set('');
    this.estudianteForm.reset();
  }

  confirmarAgregar() {
    const { email } = this.estudianteForm.value;
    this.grupoService.agregarEstudiante(this.grupoSeleccionado()!, email!).subscribe({
      next: () => {
        this.mensajeEstudiante.set('Estudiante agregado correctamente.');
        this.grupoService.listar().subscribe(g => this.grupos.set(g));
      },
      error: () => this.mensajeEstudiante.set('Error: verifique el email del estudiante.')
    });
  }
}
