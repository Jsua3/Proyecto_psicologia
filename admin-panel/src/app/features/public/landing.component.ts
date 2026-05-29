import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { APP_BRAND } from '../../core/config/brand.config';

interface SectionCard {
  icon: string;
  title: string;
  text: string;
  tag: string;
}

interface Stat {
  value: string;
  label: string;
}

const PSYCHOLOGY_PROGRAM_URL =
  'https://unihumboldt.edu.co/es/pregrados/facultad-de-ciencias-humanas-y-de-la-educacion/psicologia';
const CIP_URL = 'https://unihumboldt.edu.co/es/centro-integral-de-psicologia';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule],
  template: `
    <nav class="psy-public-nav liquid-glass" aria-label="Navegación pública">
      <a class="brand" href="#inicio" [attr.aria-label]="brand.formalName">
        <img src="/assets/images/institution/logo-cue-ccaq-vertical.webp" alt="CUE Alexander Von Humboldt" width="74" height="37">
        <span>
          <strong>{{ brand.shortName }}</strong>
          <small>{{ brand.fullName }}</small>
        </span>
      </a>

      <button class="psy-icon-button nav-toggle" type="button" aria-label="Abrir menú" (click)="menuOpen.set(!menuOpen())">
        <mat-icon>{{ menuOpen() ? 'close' : 'menu' }}</mat-icon>
      </button>

      <div class="nav-links" [class.nav-links--open]="menuOpen()">
        <a href="#plataforma">Plataforma</a>
        <a href="#modulos">Módulos</a>
        <a href="#roles">Roles</a>
        <a href="#etica">Ética</a>
        <a href="#programa">Programa</a>
        <a href="#contacto">Contacto</a>
        <a class="portal-link" routerLink="/login">Ingresar al sistema</a>
      </div>
    </nav>

    <main class="psy-page landing" id="inicio">
      <section class="hero" aria-labelledby="hero-title">
        <div class="hero-overlay"></div>
        <div class="hero-content psy-reveal">
          <p class="psy-eyebrow">{{ brand.institution }}</p>
          <h1 id="hero-title" class="psy-title">{{ brand.shortName }}</h1>
          <p class="hero-fullname">{{ brand.fullName }}</p>
          <p class="psy-subtitle">{{ brand.subtitle }}</p>
          <p class="hero-description">{{ brand.description }}</p>
          <div class="hero-badges" aria-label="Enfoque académico">
            <span class="psy-chip">Simulación formativa</span>
            <span class="psy-chip">Trazabilidad docente</span>
            <span class="psy-chip">Evaluación con rúbricas</span>
          </div>
          <div class="psy-action-row hero-actions">
            <a class="psy-button psy-button--primary" routerLink="/login">
              <mat-icon>login</mat-icon>
              Ingresar al sistema
            </a>
            <a class="psy-button psy-button--ghost" href="#modulos">
              <mat-icon>view_module</mat-icon>
              Ver módulos
            </a>
            <a class="psy-button psy-button--ghost" [href]="psychologyProgramUrl" target="_blank" rel="noopener">
              <mat-icon>school</mat-icon>
              Programa de Psicología
            </a>
          </div>
        </div>

        <a class="next-cue" href="#plataforma" aria-label="Conocer la plataforma">
          <mat-icon>keyboard_arrow_down</mat-icon>
        </a>
      </section>

      <section id="plataforma" class="psy-section soft-band">
        <div class="psy-section__inner split">
          <div>
            <p class="psy-eyebrow">Propósito académico</p>
            <h2 class="section-title">Entrenamiento psicosocial con trazabilidad y evaluación formativa.</h2>
          </div>
          <p class="section-copy">{{ brand.objective }}</p>
        </div>
        <div class="psy-section__inner psy-grid psy-grid--3 card-row">
          @for (benefit of benefits; track benefit.title) {
            <article class="siep-card">
              <mat-icon>{{ benefit.icon }}</mat-icon>
              <h3>{{ benefit.title }}</h3>
              <p>{{ benefit.text }}</p>
            </article>
          }
        </div>
      </section>

      <section id="modulos" class="psy-section">
        <div class="psy-section__inner split">
          <div>
            <p class="psy-eyebrow">Módulos</p>
            <h2 class="section-title">Simulación, bitácora, trazabilidad, rúbricas y reportes.</h2>
          </div>
          <p class="section-copy">
            SIEP integra los componentes necesarios para practicar decisiones en casos simulados, documentar el
            proceso reflexivo y facilitar la evaluación docente con evidencia trazable.
          </p>
        </div>
        <div class="psy-section__inner psy-grid psy-grid--3 card-row">
          @for (module of modules; track module.title) {
            <article class="psy-card liquid-tilt">
              <span class="psy-chip">{{ module.tag }}</span>
              <mat-icon>{{ module.icon }}</mat-icon>
              <h3>{{ module.title }}</h3>
              <p>{{ module.text }}</p>
            </article>
          }
        </div>
      </section>

      <section id="roles" class="psy-section soft-band">
        <div class="psy-section__inner split">
          <div>
            <p class="psy-eyebrow">Roles</p>
            <h2 class="section-title">Estudiante, profesor y administrador.</h2>
          </div>
          <p class="section-copy">
            Cada perfil accede a funcionalidades acordes con su responsabilidad formativa, de supervisión o de
            gestión institucional del sistema.
          </p>
        </div>
        <div class="psy-section__inner psy-grid psy-grid--3 card-row">
          @for (role of roles; track role.title) {
            <article class="siep-card role-card">
              <mat-icon>{{ role.icon }}</mat-icon>
              <h3>{{ role.title }}</h3>
              <p>{{ role.text }}</p>
            </article>
          }
        </div>
      </section>

      <section id="etica" class="psy-section">
        <div class="psy-section__inner support-layout">
          <div>
            <p class="psy-eyebrow">Compromiso ético</p>
            <h2 class="section-title">Casos sensibles, salida segura y evaluación formativa.</h2>
            <p class="section-copy">
              SIEP prioriza la reflexión ética, el lenguaje no estigmatizante y rutas de atención institucionales
              cuando el contenido simulado lo requiera.
            </p>
          </div>
          <div class="support-panel liquid-glass">
            @for (item of ethicsItems; track item) {
              <div class="support-item">
                <mat-icon>verified_user</mat-icon>
                <span>{{ item }}</span>
              </div>
            }
          </div>
        </div>
      </section>

      <section id="programa" class="psy-section">
        <div class="psy-section__inner split">
          <div>
            <p class="psy-eyebrow">Pregrado acreditado</p>
            <h2 class="section-title">Psicología con excelencia académica, práctica y sentido social.</h2>
          </div>
          <p class="section-copy">
            El programa articula teoría, investigación, laboratorios, práctica profesional, proyección social y
            formación ética. Su modelo pedagógico promueve construcción colectiva del conocimiento, autonomía
            estudiantil y lectura crítica del territorio.
          </p>
        </div>

        <div class="psy-section__inner official-strip liquid-glass">
          <div>
            <span class="psy-chip">Sello ASCOFAPSI</span>
            <h3>Reconocimiento disciplinar y cultura de calidad.</h3>
          </div>
          <img src="/assets/images/institution/ascofapsi-logo.png" alt="Sello ASCOFAPSI" width="150" height="83">
        </div>

        <div class="psy-section__inner psy-grid psy-grid--3 card-row">
          @for (program of programs; track program.title) {
            <article class="psy-card liquid-tilt">
              <span class="psy-chip">{{ program.tag }}</span>
              <mat-icon>{{ program.icon }}</mat-icon>
              <h3>{{ program.title }}</h3>
              <p>{{ program.text }}</p>
            </article>
          }
        </div>
      </section>

      <section id="clinica" class="psy-section soft-band">
        <div class="psy-section__inner cip-layout">
          <div class="cip-copy">
            <p class="psy-eyebrow">Centro Integral de Psicología</p>
            <h2 class="section-title">Atención, evaluación y acompañamiento psicológico desde la universidad.</h2>
            <p class="section-copy">
              El CIP integra servicios de atención psicológica, neuropsicología y espacios formativos para la
              comunidad, con un enfoque de cuidado, confidencialidad, supervisión académica y rutas institucionales.
            </p>
            <div class="psy-action-row">
              <a class="psy-button psy-button--primary" [href]="cipUrl" target="_blank" rel="noopener">
                <mat-icon>open_in_new</mat-icon>
                Ver CIP oficial
              </a>
              <a class="psy-button psy-button--ghost" href="mailto:secretariacip@cue.edu.co">
                <mat-icon>mail</mat-icon>
                secretariacip@cue.edu.co
              </a>
            </div>
          </div>
          <div class="cip-panel liquid-glass" aria-label="Indicadores del Centro Integral de Psicología">
            <img src="/assets/images/institution/cip-banner.webp" alt="Imagen del Centro Integral de Psicología" width="620" height="349">
            <div class="cip-stats">
              @for (stat of cipStats; track stat.label) {
                <div>
                  <strong>{{ stat.value }}</strong>
                  <span>{{ stat.label }}</span>
                </div>
              }
            </div>
          </div>
        </div>
      </section>

      <section id="investigacion" class="psy-section">
        <div class="psy-section__inner split">
          <div>
            <p class="psy-eyebrow">Investigación y semilleros</p>
            <h2 class="section-title">Preguntas vivas sobre salud mental, comunidad y aprendizaje.</h2>
          </div>
          <p class="section-copy">
            La Humboldt conecta dirección de investigaciones, semilleros, grupos y líneas de investigación con
            prácticas formativas del programa. SIEP extiende esa vocación mediante simulación evaluable y trazabilidad académica.
          </p>
        </div>
        <div class="psy-section__inner psy-grid psy-grid--4 card-row">
          @for (line of researchLines; track line.title) {
            <article class="psy-card liquid-tilt compact-card">
              <mat-icon>{{ line.icon }}</mat-icon>
              <h3>{{ line.title }}</h3>
              <p>{{ line.text }}</p>
            </article>
          }
        </div>
      </section>

      <section id="bienestar" class="psy-section soft-band">
        <div class="psy-section__inner support-layout">
          <div>
            <p class="psy-eyebrow">Bienestar y rutas de apoyo</p>
            <h2 class="section-title">Una experiencia académica que escucha y orienta.</h2>
            <p class="section-copy">
              El portal debe mantener salida segura, lenguaje no estigmatizante, confidencialidad y recursos de
              apoyo para estudiantes, docentes, consultantes y comunidad.
            </p>
          </div>
          <div class="support-panel liquid-glass">
            @for (support of supportRoutes; track support) {
              <div class="support-item">
                <mat-icon>check_circle</mat-icon>
                <span>{{ support }}</span>
              </div>
            }
          </div>
        </div>
      </section>

      <section class="psy-section">
        <div class="psy-section__inner psy-grid psy-grid--3">
          <article class="psy-card editorial-card" id="docentes">
            <span class="psy-chip">Docentes y colaboradores</span>
            <h3>Equipo académico con trayectoria clínica, jurídica, social e investigativa.</h3>
            <p>La página oficial del programa reúne docentes, coordinación académica, prácticas, laboratorios, CIP e investigación.</p>
          </article>
          <article class="psy-card editorial-card" id="eventos">
            <span class="psy-chip">Eventos y noticias</span>
            <h3>Seminarios y conversaciones que llevan el aula al territorio.</h3>
            <p>La agenda Humboldt incluye espacios de investigación, salud mental, psicología y divulgación académica.</p>
          </article>
          <article class="psy-card editorial-card" id="admisiones">
            <span class="psy-chip">Admisiones</span>
            <h3>Ingreso semestral con orientación para aspirantes y familias.</h3>
            <p>El programa solicita entrevista, examen de admisión, soporte documental y proceso de preinscripción institucional.</p>
          </article>
        </div>
      </section>

      <section id="contacto" class="psy-section contact-section">
        <div class="psy-section__inner contact-card liquid-glass">
          <div>
            <p class="psy-eyebrow">Contacto institucional</p>
            <h2 class="section-title">{{ brand.formalName }}</h2>
            <p class="section-copy">
              Plataforma académica del {{ brand.program }} de la {{ brand.institution }}.
              Para simulación formativa, seguimiento docente, bitácoras reflexivas y evaluación con rúbricas.
            </p>
          </div>
          <div class="contact-actions">
            <a class="psy-button psy-button--primary" href="mailto:secretariapsicologia@cue.edu.co">
              <mat-icon>mail</mat-icon>
              secretariapsicologia@cue.edu.co
            </a>
            <a class="psy-button psy-button--ghost" [href]="psychologyProgramUrl" target="_blank" rel="noopener">
              <mat-icon>open_in_new</mat-icon>
              Sitio oficial
            </a>
          </div>
        </div>
      </section>
    </main>
  `,
  styles: [`
    .brand {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      color: var(--psy-ink);
      min-height: 44px;
    }
    .brand img {
      width: 74px;
      height: auto;
      object-fit: contain;
      filter: drop-shadow(0 8px 14px rgba(36,50,58,.12));
    }
    .brand strong,
    .brand small {
      display: block;
      line-height: 1.1;
    }
    .brand strong { font-size: .98rem; }
    .brand small { color: var(--psy-muted); font-size: .74rem; margin-top: 2px; }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .nav-links a {
      min-height: 44px;
      display: inline-flex;
      align-items: center;
      padding: 0 13px;
      border-radius: 999px;
      color: var(--psy-ink);
      font-weight: 600;
      font-size: .92rem;
    }
    .nav-links a:hover,
    .portal-link {
      background: rgba(79,124,172,.1);
    }
    .nav-toggle { display: none; }
    .hero {
      position: relative;
      min-height: 92vh;
      display: grid;
      align-items: center;
      padding: calc(var(--psy-header-h) + 56px) clamp(18px, 5vw, 70px) 80px;
      overflow: hidden;
      background:
        linear-gradient(90deg, rgba(242,242,242,.97) 0%, rgba(242,242,242,.88) 42%, rgba(232,238,243,.55) 100%),
        linear-gradient(180deg, rgba(0,72,118,.06), rgba(11,90,138,.1)),
        url('/assets/images/institution/psychology-program-hero.png') center/cover no-repeat;
    }
    .hero-overlay {
      position: absolute;
      inset: 0;
      background: linear-gradient(90deg, rgba(242,242,242,.18), rgba(255,255,255,.08));
      pointer-events: none;
    }
    .hero-content {
      position: relative;
      z-index: 1;
      width: min(820px, 100%);
    }
    .hero .psy-title { max-width: 760px; }
    .hero-fullname {
      margin: 10px 0 0;
      color: var(--siep-blue-soft);
      font-size: clamp(1.05rem, 2vw, 1.35rem);
      font-weight: 700;
    }
    .hero-description {
      max-width: 720px;
      margin: 16px 0 0;
      color: var(--siep-muted);
      font-size: 1rem;
      line-height: 1.68;
    }
    .hero-badges {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 22px;
    }
    .hero-actions { margin-top: 30px; }
    .metrics {
      position: absolute;
      right: clamp(18px, 5vw, 70px);
      bottom: 28px;
      z-index: 2;
      display: grid;
      grid-template-columns: repeat(4, minmax(104px, 1fr));
      gap: 1px;
      width: min(720px, calc(100% - 36px));
      border-radius: 18px;
      overflow: hidden;
    }
    .metric {
      padding: 18px 16px;
      background: rgba(255,255,255,.38);
    }
    .metric strong {
      display: block;
      color: var(--psy-blue-deep);
      font-size: clamp(1.35rem, 3vw, 2.05rem);
      line-height: 1;
    }
    .metric span {
      display: block;
      margin-top: 6px;
      color: var(--psy-muted);
      font-size: .84rem;
      line-height: 1.3;
    }
    .next-cue {
      position: absolute;
      left: 50%;
      bottom: 26px;
      z-index: 3;
      transform: translateX(-50%);
      display: grid;
      place-items: center;
      width: 46px;
      height: 46px;
      border-radius: 50%;
      color: var(--psy-blue-deep);
      background: rgba(255,255,255,.68);
      border: 1px solid var(--psy-border);
      animation: psy-focus-pulse 2.8s infinite;
    }
    .split {
      display: grid;
      grid-template-columns: minmax(0, .9fr) minmax(300px, .8fr);
      gap: 42px;
      align-items: end;
    }
    .section-title {
      margin: 0;
      font-family: 'Poppins', system-ui, sans-serif;
      font-weight: 800;
      font-size: clamp(1.85rem, 4vw, 3rem);
      line-height: 1.08;
      letter-spacing: 0;
      color: var(--siep-blue);
    }
    .section-copy {
      margin: 0;
      color: var(--psy-muted);
      font-size: 1.05rem;
      line-height: 1.7;
    }
    .official-strip {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
      margin-top: 28px;
      padding: 22px;
      border-radius: 18px;
    }
    .official-strip h3 {
      margin: 12px 0 0;
      font-family: 'Poppins', system-ui, sans-serif;
      font-size: clamp(1.45rem, 3vw, 2rem);
      line-height: 1.05;
    }
    .official-strip img {
      width: 150px;
      height: auto;
      object-fit: contain;
    }
    .card-row { margin-top: 26px; }
    .role-card mat-icon,
    .siep-card mat-icon {
      color: var(--siep-blue);
      font-size: 28px;
      width: 28px;
      height: 28px;
    }
    .psy-card mat-icon {
      margin-top: 18px;
      color: var(--psy-teal-deep);
    }
    .soft-band {
      background: linear-gradient(180deg, rgba(234,241,244,.62), rgba(244,247,250,.36));
    }
    .cip-layout {
      display: grid;
      grid-template-columns: minmax(0, .8fr) minmax(360px, .75fr);
      gap: 34px;
      align-items: center;
    }
    .cip-copy {
      display: grid;
      gap: 24px;
    }
    .cip-panel {
      overflow: hidden;
      border-radius: 22px;
      padding: 0;
    }
    .cip-panel img {
      display: block;
      width: 100%;
      aspect-ratio: 16 / 9;
      object-fit: cover;
    }
    .cip-stats {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 1px;
      background: rgba(47,95,143,.12);
    }
    .cip-stats div {
      padding: 18px;
      background: rgba(255,255,255,.66);
    }
    .cip-stats strong {
      display: block;
      color: var(--psy-blue-deep);
      font-size: 2rem;
      line-height: 1;
    }
    .cip-stats span {
      display: block;
      margin-top: 6px;
      color: var(--psy-muted);
      line-height: 1.35;
    }
    .compact-card { min-height: 230px; }
    .support-layout {
      display: grid;
      grid-template-columns: minmax(0, .9fr) minmax(320px, .65fr);
      gap: 32px;
      align-items: center;
    }
    .support-panel {
      display: grid;
      gap: 10px;
      padding: 18px;
      border-radius: 18px;
    }
    .support-item {
      display: flex;
      align-items: center;
      gap: 10px;
      min-height: 48px;
      padding: 12px 14px;
      border-radius: 14px;
      background: rgba(255,255,255,.58);
      color: var(--psy-ink);
      font-weight: 600;
    }
    .support-item mat-icon {
      color: var(--psy-green-deep);
      flex: 0 0 auto;
    }
    .editorial-card {
      min-height: 250px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .editorial-card h3 {
      font-family: 'Poppins', system-ui, sans-serif;
      font-size: 1.72rem;
      line-height: 1.05;
    }
    .contact-section { padding-top: 24px; }
    .contact-card {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto;
      gap: 24px;
      align-items: center;
      padding: clamp(26px, 5vw, 50px);
      border-radius: 20px;
    }
    .contact-actions {
      display: grid;
      gap: 12px;
      min-width: 300px;
    }
    @media (max-width: 1020px) {
      .brand small { display: none; }
      .nav-links a {
        padding-inline: 10px;
      }
    }
    @media (max-width: 920px) {
      .nav-toggle { display: inline-grid; }
      .nav-links {
        position: absolute;
        top: calc(100% + 10px);
        left: 0;
        right: 0;
        display: none;
        padding: 12px;
        border-radius: 22px;
        background: rgba(255,255,255,.9);
        border: 1px solid var(--psy-border);
        box-shadow: var(--psy-shadow-soft);
      }
      .nav-links--open {
        display: grid;
      }
      .nav-links a {
        justify-content: center;
      }
      .hero {
        min-height: 96vh;
        padding-bottom: 170px;
      }
      .metrics {
        left: 18px;
        right: 18px;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        width: auto;
      }
      .split,
      .cip-layout,
      .support-layout,
      .contact-card {
        grid-template-columns: 1fr;
      }
      .contact-actions {
        min-width: 0;
      }
    }
    @media (max-width: 560px) {
      .psy-public-nav {
        top: 10px;
        width: calc(100% - 20px);
      }
      .brand img { width: 58px; }
      .hero {
        padding-top: 122px;
        background-position: 56% center;
      }
      .metrics,
      .cip-stats {
        grid-template-columns: 1fr 1fr;
      }
      .metric {
        padding: 14px 12px;
      }
      .hero-actions .psy-button,
      .cip-copy .psy-button {
        width: 100%;
      }
      .official-strip {
        display: grid;
      }
      .next-cue {
        display: none;
      }
    }
  `]
})
export class LandingComponent {
  readonly brand = APP_BRAND;
  readonly menuOpen = signal(false);
  readonly psychologyProgramUrl = PSYCHOLOGY_PROGRAM_URL;
  readonly cipUrl = CIP_URL;

  readonly benefits: SectionCard[] = [
    {
      icon: 'psychology',
      title: 'Práctica deliberada',
      text: 'Entrenamiento psicosocial mediante casos simulados con decisiones trazables y retroalimentación formativa.',
      tag: ''
    },
    {
      icon: 'menu_book',
      title: 'Bitácoras reflexivas',
      text: 'Registro estructurado del proceso de reflexión ética y analítica durante cada intento.',
      tag: ''
    },
    {
      icon: 'fact_check',
      title: 'Evaluación docente',
      text: 'Rúbricas académicas e informes formativos para el seguimiento del desempeño estudiantil.',
      tag: ''
    }
  ];

  readonly modules: SectionCard[] = [
    { icon: 'play_circle', title: 'Simulación', text: 'Casos psicosociales con rutas de decisión y retroalimentación formativa.', tag: 'Módulo' },
    { icon: 'edit_note', title: 'Bitácora', text: 'Documentación reflexiva del proceso de toma de decisiones.', tag: 'Módulo' },
    { icon: 'timeline', title: 'Trazabilidad', text: 'Registro docente de eventos, decisiones y métricas del intento.', tag: 'Módulo' },
    { icon: 'grading', title: 'Rúbricas', text: 'Evaluación formativa con criterios académicos definidos.', tag: 'Módulo' },
    { icon: 'analytics', title: 'Reportes', text: 'Indicadores de seguimiento para cohortes y desempeño general.', tag: 'Módulo' },
    { icon: 'shield', title: 'Salida segura', text: 'Rutas de atención y recursos cuando el contenido simulado lo requiera.', tag: 'Módulo' }
  ];

  readonly roles: SectionCard[] = [
    { icon: 'school', title: 'Estudiante', text: 'Practica decisiones en casos simulados, completa bitácoras y recibe retroalimentación formativa.', tag: '' },
    { icon: 'supervisor_account', title: 'Profesor', text: 'Supervisa trazabilidad, evalúa con rúbricas y acompaña el proceso formativo.', tag: '' },
    { icon: 'admin_panel_settings', title: 'Administrador', text: 'Gestiona usuarios, casos, publicaciones y configuración académica del sistema.', tag: '' }
  ];

  readonly ethicsItems = [
    'Manejo responsable de casos sensibles con salida segura.',
    'Evaluación formativa, sin decisiones automatizadas.',
    'Confidencialidad y trazabilidad ética de bitácoras.',
    'Lenguaje claro, humano y centrado en derechos.',
    'Rutas de atención institucionales disponibles.'
  ];

  readonly metrics: Stat[] = [
    { value: '101645', label: 'Código SNIES' },
    { value: '8', label: 'semestres' },
    { value: '160', label: 'créditos académicos' },
    { value: '6 años', label: 'acreditación alta calidad' }
  ];

  readonly cipStats: Stat[] = [
    { value: '735', label: 'personas atendidas' },
    { value: '1230', label: 'atenciones efectivas' }
  ];

  readonly programs: SectionCard[] = [
    {
      icon: 'psychology',
      title: 'Psicólogo(a)',
      text: 'Título profesional universitario con formación integral en evaluación, intervención, investigación y ética.',
      tag: 'Título'
    },
    {
      icon: 'location_city',
      title: 'Presencial en Armenia',
      text: 'Jornada diurna, ingreso semestral y experiencias formativas conectadas con contextos reales del territorio.',
      tag: 'Modalidad'
    },
    {
      icon: 'hub',
      title: 'Modelo constructivista',
      text: 'Aprendizaje participativo e interactivo para aplicar conocimiento psicológico en diversos contextos.',
      tag: 'Pedagogía'
    }
  ];

  readonly researchLines: SectionCard[] = [
    { icon: 'monitoring', title: 'Salud mental', text: 'Lectura de riesgos, bienestar y rutas de apoyo basadas en evidencia.', tag: '' },
    { icon: 'diversity_3', title: 'Psicología social', text: 'Comunidad, convivencia, conflicto, derechos humanos y territorio.', tag: '' },
    { icon: 'science', title: 'Métodos y datos', text: 'Medición, evaluación, análisis de casos y trazabilidad académica.', tag: '' },
    { icon: 'school', title: 'Simulación', text: 'Práctica deliberada, bitácoras, rúbricas y retroalimentación docente.', tag: '' }
  ];

  readonly supportRoutes = [
    'Salida segura y recursos institucionales en casos sensibles.',
    'Bitácoras protegidas, confidencialidad y trazabilidad ética.',
    'Lenguaje claro, humano y centrado en derechos.',
    'Acompañamiento para estudiantes, docentes, consultantes y comunidad.'
  ];
}
