import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormArray, ReactiveFormsModule, Validators, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatStepperModule } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { CasoService } from '../../core/api/caso.service';
import { CasoRequest } from '../../core/models/caso.model';

@Component({
  selector: 'app-caso-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatStepperModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatCheckboxModule, MatIconModule, MatCardModule
  ],
  template: `
    <div class="page-header">
      <h1 class="page-title">{{ esEdicion ? 'Editar' : 'Nuevo' }} Caso Clínico</h1>
      <a mat-stroked-button routerLink="/casos">Volver</a>
    </div>

    <mat-stepper [linear]="true" #stepper>
      <!-- Paso 1: Datos generales -->
      <mat-step [stepControl]="paso1">
        <ng-template matStepLabel>Datos generales</ng-template>
        <form [formGroup]="paso1" class="step-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Título del caso</mat-label>
            <input matInput formControlName="titulo">
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Descripción breve</mat-label>
            <textarea matInput formControlName="descripcion" rows="3"></textarea>
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Contexto narrativo</mat-label>
            <textarea matInput formControlName="contextoNarrativo" rows="6"></textarea>
          </mat-form-field>
          <div class="step-actions">
            <button mat-flat-button color="primary" matStepperNext [disabled]="paso1.invalid">Siguiente</button>
          </div>
        </form>
      </mat-step>

      <!-- Paso 2: Escenarios -->
      <mat-step>
        <ng-template matStepLabel>Escenarios</ng-template>
        <div class="step-form">
          <div *ngFor="let esc of escenarios.controls; let i = index" class="escenario-block">
            <h3>Escenario {{ i + 1 }}</h3>
            <ng-container [formGroup]="asGroup(esc)">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nombre</mat-label>
                <input matInput formControlName="nombre">
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Clave del mapa (hospital / comisaria)</mat-label>
                <input matInput formControlName="mapaKey">
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Contexto del escenario</mat-label>
                <textarea matInput formControlName="contexto" rows="3"></textarea>
              </mat-form-field>
            </ng-container>
            <button mat-stroked-button color="warn" (click)="escenarios.removeAt(i)">
              <mat-icon>delete</mat-icon> Eliminar escenario
            </button>
          </div>
          <button mat-stroked-button (click)="agregarEscenario()">
            <mat-icon>add</mat-icon> Agregar escenario
          </button>
          <div class="step-actions">
            <button mat-stroked-button matStepperPrevious>Atrás</button>
            <button mat-flat-button color="primary" matStepperNext>Siguiente</button>
          </div>
        </div>
      </mat-step>

      <!-- Paso 3: Preguntas y opciones -->
      <mat-step>
        <ng-template matStepLabel>Preguntas y opciones</ng-template>
        <div class="step-form">
          <div *ngFor="let esc of escenarios.controls; let ei = index" class="escenario-section">
            <h3>{{ asGroup(esc).get('nombre')?.value || 'Escenario ' + (ei+1) }}</h3>
            <ng-container [formGroup]="asGroup(esc)">
              <div *ngFor="let preg of getPreguntas(ei).controls; let pi = index" class="pregunta-block">
                <h4>Pregunta {{ pi + 1 }}</h4>
                <ng-container [formGroup]="asGroup(preg)">
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Enunciado</mat-label>
                    <textarea matInput formControlName="enunciado" rows="3"></textarea>
                  </mat-form-field>
                  <div *ngFor="let op of getOpciones(ei, pi).controls; let oi = index" class="opcion-block">
                    <ng-container [formGroup]="asGroup(op)">
                      <mat-form-field appearance="outline" style="width:60%">
                        <mat-label>Opción {{ ['A','B','C','D'][oi] }}</mat-label>
                        <input matInput formControlName="texto">
                      </mat-form-field>
                      <mat-checkbox formControlName="esCorrecta" color="primary"> Correcta</mat-checkbox>
                      <mat-form-field appearance="outline" class="full-width">
                        <mat-label>Feedback</mat-label>
                        <textarea matInput formControlName="feedbackTexto" rows="2"></textarea>
                      </mat-form-field>
                      <mat-form-field appearance="outline" class="full-width">
                        <mat-label>Referencia normativa</mat-label>
                        <input matInput formControlName="normativaRef">
                      </mat-form-field>
                    </ng-container>
                  </div>
                  <button mat-stroked-button (click)="agregarOpcion(ei, pi)" [disabled]="getOpciones(ei,pi).length >= 4">
                    <mat-icon>add</mat-icon> Opción
                  </button>
                  <button mat-stroked-button color="warn" (click)="getPreguntas(ei).removeAt(pi)">
                    <mat-icon>delete</mat-icon> Pregunta
                  </button>
                </ng-container>
              </div>
              <button mat-stroked-button (click)="agregarPregunta(ei)">
                <mat-icon>add</mat-icon> Agregar pregunta
              </button>
            </ng-container>
          </div>

          <div class="step-actions">
            <button mat-stroked-button matStepperPrevious>Atrás</button>
            <button mat-flat-button color="primary" (click)="guardar()" [disabled]="saving()">
              {{ saving() ? 'Guardando...' : (esEdicion ? 'Actualizar' : 'Crear caso') }}
            </button>
          </div>
        </div>
      </mat-step>
    </mat-stepper>
  `,
  styles: [`
    .page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
    .page-title { font-size: 24px; font-weight: 700; color: #1A2B3C; margin: 0; }
    .step-form { padding: 16px 0; }
    .full-width { width: 100%; }
    .step-actions { display: flex; gap: 12px; margin-top: 24px; }
    .escenario-block, .escenario-section { border: 1px solid #e0e6ed; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
    .pregunta-block { border-left: 3px solid #7A9EC0; padding-left: 16px; margin: 12px 0; }
    .opcion-block { background: #f8f9fa; border-radius: 6px; padding: 12px; margin: 8px 0; }
  `]
})
export class CasoFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private casoService = inject(CasoService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  esEdicion = false;
  casoId: number | null = null;
  saving = signal(false);

  paso1 = this.fb.group({
    titulo: ['', Validators.required],
    descripcion: [''],
    contextoNarrativo: ['']
  });

  escenariosForm = this.fb.group({ escenarios: this.fb.array([]) });
  get escenarios() { return this.escenariosForm.get('escenarios') as FormArray; }

  asGroup(c: unknown): FormGroup { return c as FormGroup; }
  getPreguntas(ei: number) { return this.asGroup(this.escenarios.at(ei)).get('preguntas') as FormArray; }
  getOpciones(ei: number, pi: number) { return this.asGroup(this.getPreguntas(ei).at(pi)).get('opciones') as FormArray; }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.esEdicion = true;
      this.casoId = +id;
      this.casoService.obtener(this.casoId).subscribe(caso => {
        this.paso1.patchValue(caso);
        caso.escenarios?.forEach(e => {
          const escGroup = this.crearEscenarioGroup();
          escGroup.patchValue(e);
          this.escenarios.push(escGroup);
        });
      });
    } else {
      this.agregarEscenario();
    }
  }

  crearEscenarioGroup(): FormGroup {
    return this.fb.group({
      orden: [this.escenarios.length + 1],
      nombre: ['', Validators.required],
      mapaKey: ['hospital', Validators.required],
      contexto: [''],
      preguntas: this.fb.array([])
    });
  }

  agregarEscenario() { this.escenarios.push(this.crearEscenarioGroup()); }

  agregarPregunta(ei: number) {
    const pregs = this.getPreguntas(ei);
    pregs.push(this.fb.group({
      orden: [pregs.length + 1],
      enunciado: ['', Validators.required],
      puntosCorrecta: [10],
      opciones: this.fb.array([this.crearOpcionGroup(), this.crearOpcionGroup(), this.crearOpcionGroup()])
    }));
  }

  agregarOpcion(ei: number, pi: number) {
    this.getOpciones(ei, pi).push(this.crearOpcionGroup());
  }

  crearOpcionGroup(): FormGroup {
    return this.fb.group({ texto: [''], esCorrecta: [false], feedbackTexto: [''], normativaRef: [''] });
  }

  guardar() {
    this.saving.set(true);
    const req: CasoRequest = {
      ...this.paso1.value as { titulo: string; descripcion: string; contextoNarrativo: string },
      escenarios: this.escenarios.value
    };
    const op = this.esEdicion
      ? this.casoService.actualizar(this.casoId!, req)
      : this.casoService.crear(req);

    op.subscribe({
      next: () => this.router.navigate(['/casos']),
      error: () => this.saving.set(false)
    });
  }
}
