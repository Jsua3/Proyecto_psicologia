import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule
  ],
  template: `
    <main class="login-page">
      <section class="login-visual" aria-label="Escena institucional de psicología universitaria">
        <div class="visual-copy">
          <a routerLink="/" class="back-link">
            <mat-icon>arrow_back</mat-icon>
            Psicología Humboldt
          </a>
          <img class="visual-logo" src="/assets/images/institution/logo-cue-ccaq-vertical.webp" alt="CUE Alexander Von Humboldt" width="160" height="80">
          <h1>Portal académico PsychoSim</h1>
          <p>
            Acceso seguro para simulación, seguimiento docente, analíticas formativas y gestión ética de casos
            del Programa de Psicología.
          </p>
        </div>
      </section>

      <section class="login-form-zone">
        <article class="psy-auth-card liquid-glass">
          <div class="form-heading">
            <span class="psy-chip">Acceso institucional</span>
            <h2>Ingresa con tus credenciales</h2>
            <p>Tu sesión protege información académica y contenidos sensibles.</p>
          </div>

          <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
            <mat-form-field appearance="outline">
              <mat-label>Correo institucional</mat-label>
              <mat-icon matPrefix>mail</mat-icon>
              <input matInput type="email" formControlName="email" autocomplete="email" placeholder="usuario@cue.edu.co">
              @if (form.controls.email.hasError('required')) {
                <mat-error>El correo es obligatorio.</mat-error>
              }
              @if (form.controls.email.hasError('email')) {
                <mat-error>Ingresa un correo institucional válido.</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Contraseña</mat-label>
              <mat-icon matPrefix>lock</mat-icon>
              <input matInput [type]="hidePassword() ? 'password' : 'text'" formControlName="password" autocomplete="current-password" placeholder="Contraseña del portal">
              <button mat-icon-button matSuffix type="button" [attr.aria-label]="hidePassword() ? 'Mostrar contraseña' : 'Ocultar contraseña'" (click)="hidePassword.set(!hidePassword())">
                <mat-icon>{{ hidePassword() ? 'visibility' : 'visibility_off' }}</mat-icon>
              </button>
              @if (form.controls.password.hasError('required')) {
                <mat-error>La contraseña es obligatoria.</mat-error>
              }
            </mat-form-field>

            <div class="form-options">
              <a href="mailto:secretariapsicologia@cue.edu.co">Recuperar contraseña</a>
              <span class="psy-mono">SSO habilitado</span>
            </div>

            @if (error()) {
              <p class="error-msg" role="alert">{{ error() }}</p>
            }

            @if (loading()) {
              <mat-progress-bar mode="indeterminate"></mat-progress-bar>
            }

            <button class="psy-button psy-button--primary submit-btn" type="submit" [disabled]="form.invalid || loading()">
              <mat-icon>login</mat-icon>
              Ingresar al portal
            </button>
          </form>

          <div class="sso-row" aria-label="Accesos SSO institucionales">
            <button class="psy-button psy-button--ghost" type="button">
              <mat-icon>account_circle</mat-icon>
              Continuar con Google
            </button>
            <button class="psy-button psy-button--ghost" type="button">
              <mat-icon>domain</mat-icon>
              Continuar con Microsoft
            </button>
          </div>
        </article>
      </section>
    </main>
  `,
  styles: [`
    .login-page {
      min-height: 100vh;
      display: grid;
      grid-template-columns: minmax(420px, .92fr) minmax(430px, 1fr);
      background:
        radial-gradient(circle at 72% 18%, rgba(169,155,214,.2), transparent 30%),
        linear-gradient(135deg, #F7FAFC, #EAF1F4);
    }
    .login-visual {
      position: relative;
      display: flex;
      align-items: flex-end;
      min-height: 100vh;
      padding: clamp(28px, 5vw, 58px);
      overflow: hidden;
      background:
        linear-gradient(180deg, rgba(36,50,58,.08), rgba(36,50,58,.68)),
        url('/assets/images/institution/psychology-program-hero.png') center/cover no-repeat;
    }
    .login-visual::after {
      content: '';
      position: absolute;
      inset: 0;
      background:
        radial-gradient(circle at 30% 22%, rgba(79,163,165,.32), transparent 34%),
        radial-gradient(circle at 78% 74%, rgba(169,155,214,.28), transparent 30%);
      pointer-events: none;
    }
    .visual-copy {
      position: relative;
      z-index: 1;
      max-width: 640px;
      color: white;
    }
    .visual-logo {
      width: 160px;
      height: auto;
      margin-top: 28px;
      padding: 12px;
      border-radius: 18px;
      background: rgba(255,255,255,.84);
      border: 1px solid rgba(255,255,255,.36);
      box-shadow: 0 22px 44px -28px rgba(0,0,0,.48);
    }
    .back-link {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      min-height: 44px;
      padding: 0 14px;
      border-radius: 999px;
      color: white;
      background: rgba(255,255,255,.16);
      border: 1px solid rgba(255,255,255,.24);
      backdrop-filter: blur(16px);
      font-weight: 700;
    }
    .visual-copy h1 {
      margin: 28px 0 12px;
      font-family: 'Cormorant Garamond', serif;
      font-size: clamp(2.5rem, 5vw, 5rem);
      line-height: .96;
      letter-spacing: 0;
    }
    .visual-copy p {
      max-width: 560px;
      margin: 0;
      color: rgba(255,255,255,.86);
      font-size: 1.1rem;
      line-height: 1.65;
    }
    .login-form-zone {
      display: grid;
      place-items: center;
      padding: clamp(18px, 5vw, 64px);
    }
    .psy-auth-card {
      width: min(520px, 100%);
    }
    .form-heading {
      margin-bottom: 26px;
    }
    .form-heading h2 {
      margin: 18px 0 8px;
      font-family: 'Cormorant Garamond', serif;
      font-size: clamp(2rem, 4vw, 3rem);
      line-height: 1;
      letter-spacing: 0;
    }
    .form-heading p {
      margin: 0;
      color: var(--psy-muted);
      line-height: 1.55;
    }
    form {
      display: grid;
      gap: 14px;
    }
    mat-icon[matPrefix] {
      margin-right: 8px;
      color: var(--psy-blue-deep);
    }
    .form-options {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      align-items: center;
      color: var(--psy-muted);
      font-size: .9rem;
    }
    .form-options a {
      font-weight: 700;
    }
    .form-options span {
      font-size: .78rem;
      color: var(--psy-teal-deep);
    }
    .error-msg {
      margin: 0;
      padding: 12px 14px;
      border-radius: 12px;
      color: #8F2F3D;
      background: rgba(143,47,61,.08);
      border: 1px solid rgba(143,47,61,.14);
      line-height: 1.45;
    }
    .submit-btn {
      width: 100%;
      margin-top: 4px;
    }
    .submit-btn[disabled] {
      opacity: .62;
      cursor: not-allowed;
    }
    .sso-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-top: 18px;
    }
    .sso-row .psy-button {
      min-height: 46px;
      padding-inline: 14px;
      font-size: .92rem;
    }
    @media (max-width: 920px) {
      .login-page {
        grid-template-columns: 1fr;
      }
      .login-visual {
        display: none;
      }
      .login-form-zone {
        min-height: 100vh;
      }
    }
    @media (max-width: 520px) {
      .login-form-zone {
        padding: 16px;
      }
      .sso-row,
      .form-options {
        grid-template-columns: 1fr;
        display: grid;
      }
      .form-options {
        gap: 6px;
      }
    }
  `]
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly hidePassword = signal(true);
  readonly loading = signal(false);
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  onSubmit() {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.error.set('');

    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => this.router.navigate(['/portal/dashboard']),
      error: () => {
        this.error.set('No pudimos iniciar sesión. Revisa tus credenciales o contacta a soporte institucional.');
        this.loading.set(false);
      }
    });
  }
}
