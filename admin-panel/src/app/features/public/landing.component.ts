import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

interface SectionCard {
  icon: string;
  title: string;
  text: string;
  tag: string;
}

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule],
  template: `
    <nav class="psy-public-nav liquid-glass" aria-label="Navegación pública">
      <a class="brand" href="#inicio" aria-label="Facultad de Psicología">
        <span class="brand-mark">Ψ</span>
        <span>
          <strong>Facultad de Psicología</strong>
          <small>[NOMBRE_UNIVERSIDAD]</small>
        </span>
      </a>

      <button class="psy-icon-button nav-toggle" type="button" aria-label="Abrir menú" (click)="menuOpen.set(!menuOpen())">
        <mat-icon>{{ menuOpen() ? 'close' : 'menu' }}</mat-icon>
      </button>

      <div class="nav-links" [class.nav-links--open]="menuOpen()">
        <a href="#programas">Programas</a>
        <a href="#clinica">Atención</a>
        <a href="#investigacion">Investigación</a>
        <a href="#bienestar">Bienestar</a>
        <a href="#contacto">Contacto</a>
        <a class="portal-link" routerLink="/login">Portal académico</a>
      </div>
    </nav>

    <main class="psy-page landing" id="inicio">
      <section class="hero" aria-labelledby="hero-title">
        <div class="hero-overlay"></div>
        <div class="hero-content psy-reveal">
          <p class="psy-eyebrow">Ciencia, escucha y bienestar humano</p>
          <h1 id="hero-title" class="psy-title">Facultad de Psicología</h1>
          <p class="psy-subtitle">
            Formación, investigación, práctica profesional y servicio a la comunidad desde una mirada ética,
            rigurosa y profundamente humana.
          </p>
          <div class="psy-action-row hero-actions">
            <a class="psy-button psy-button--primary" href="#programas">
              <mat-icon>school</mat-icon>
              Conoce los programas
            </a>
            <a class="psy-button psy-button--glass" href="#contacto">
              <mat-icon>event_available</mat-icon>
              Agenda orientación
            </a>
            <a class="psy-button psy-button--ghost" href="#investigacion">
              <mat-icon>biotech</mat-icon>
              Explora investigación
            </a>
          </div>
        </div>

        <div class="metrics liquid-glass" aria-label="Indicadores de la Facultad">
          @for (metric of metrics; track metric.label) {
            <div class="metric">
              <strong>{{ metric.value }}</strong>
              <span>{{ metric.label }}</span>
            </div>
          }
        </div>

        <a class="next-cue" href="#programas" aria-label="Ir a programas académicos">
          <mat-icon>keyboard_arrow_down</mat-icon>
        </a>
      </section>

      <section id="programas" class="psy-section">
        <div class="psy-section__inner split">
          <div>
            <p class="psy-eyebrow">Programas académicos</p>
            <h2 class="section-title">Formación con rigor científico y sensibilidad social.</h2>
          </div>
          <p class="section-copy">
            Nuestros programas articulan fundamentos teóricos, laboratorios, simulación, práctica supervisada
            y aprendizaje basado en casos para formar profesionales capaces de escuchar, investigar e intervenir.
          </p>
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
        <div class="psy-section__inner feature-band liquid-glass">
          <div>
            <p class="psy-eyebrow">Clínica y centro de atención</p>
            <h2 class="section-title">Acompañamiento psicológico universitario, ético y supervisado.</h2>
            <p class="section-copy">
              El centro integra orientación, escucha inicial, rutas de apoyo y práctica profesional bajo criterios
              de confidencialidad, supervisión docente y cuidado del consultante.
            </p>
          </div>
          <div class="feature-list" aria-label="Servicios del centro">
            <span><mat-icon>psychology</mat-icon> Orientación psicológica</span>
            <span><mat-icon>groups</mat-icon> Acompañamiento comunitario</span>
            <span><mat-icon>verified_user</mat-icon> Rutas de protección</span>
          </div>
        </div>
      </section>

      <section id="investigacion" class="psy-section">
        <div class="psy-section__inner split">
          <div>
            <p class="psy-eyebrow">Investigación y semilleros</p>
            <h2 class="section-title">Preguntas vivas sobre bienestar, comunidad y aprendizaje.</h2>
          </div>
          <p class="section-copy">
            Los semilleros conectan métodos científicos, análisis de datos, intervención psicosocial y ética
            aplicada para fortalecer decisiones basadas en evidencia.
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
            <h2 class="section-title">Recursos claros para momentos que requieren cuidado.</h2>
            <p class="section-copy">
              La experiencia académica incorpora rutas de orientación, salida segura, recursos de apoyo y lenguaje
              no estigmatizante para estudiantes, consultantes y comunidad.
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
            <span class="psy-chip">Docentes y grupos</span>
            <h3>Mentoría académica con mirada interdisciplinar.</h3>
            <p>Equipos docentes acompañan laboratorios, práctica, evaluación auténtica y proyectos de impacto social.</p>
          </article>
          <article class="psy-card editorial-card" id="eventos">
            <span class="psy-chip">Eventos y noticias</span>
            <h3>Conversaciones que conectan aula, investigación y territorio.</h3>
            <p>Seminarios, jornadas, encuentros de semilleros y espacios de divulgación para la comunidad académica.</p>
          </article>
          <article class="psy-card editorial-card" id="admisiones">
            <span class="psy-chip">Admisiones</span>
            <h3>Un ingreso acompañado, claro y cercano.</h3>
            <p>Orientación vocacional, información de programas y acompañamiento para aspirantes y familias.</p>
          </article>
        </div>
      </section>

      <section id="contacto" class="psy-section contact-section">
        <div class="psy-section__inner contact-card liquid-glass">
          <div>
            <p class="psy-eyebrow">Contacto</p>
            <h2 class="section-title">Hablemos sobre tu proceso académico.</h2>
            <p class="section-copy">
              Recibe orientación sobre programas, práctica profesional, investigación, bienestar o acceso al portal.
            </p>
          </div>
          <div class="contact-actions">
            <a class="psy-button psy-button--primary" href="mailto:psicologia@universidad.edu">
              <mat-icon>mail</mat-icon>
              psicologia@universidad.edu
            </a>
            <a class="psy-button psy-button--ghost" routerLink="/login">
              <mat-icon>login</mat-icon>
              Ingresar al portal
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
    .brand-mark {
      display: grid;
      place-items: center;
      width: 42px;
      height: 42px;
      border-radius: 50%;
      background: linear-gradient(135deg, rgba(79,124,172,.18), rgba(79,163,165,.2));
      color: var(--psy-blue-deep);
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.8rem;
      font-weight: 700;
    }
    .brand strong,
    .brand small {
      display: block;
      line-height: 1.1;
    }
    .brand strong { font-size: .98rem; }
    .brand small { color: var(--psy-muted); font-size: .76rem; margin-top: 2px; }
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
      min-height: 94vh;
      display: grid;
      align-items: end;
      padding: calc(var(--psy-header-h) + 56px) clamp(18px, 5vw, 70px) 118px;
      overflow: hidden;
      background:
        linear-gradient(90deg, rgba(244,247,250,.96) 0%, rgba(244,247,250,.8) 42%, rgba(234,241,244,.42) 100%),
        linear-gradient(180deg, rgba(47,95,143,.08), rgba(79,163,165,.16)),
        url('/assets/images/psychology-faculty-hero.png') center/cover no-repeat;
    }
    .hero-overlay {
      position: absolute;
      inset: 0;
      background:
        radial-gradient(circle at 16% 30%, rgba(169,155,214,.22), transparent 30%),
        radial-gradient(circle at 78% 62%, rgba(140,191,166,.22), transparent 32%);
      pointer-events: none;
    }
    .hero-content {
      position: relative;
      z-index: 1;
      width: min(780px, 100%);
    }
    .hero .psy-title { max-width: 720px; }
    .hero-actions { margin-top: 30px; }
    .metrics {
      position: absolute;
      right: clamp(18px, 5vw, 70px);
      bottom: 28px;
      z-index: 2;
      display: grid;
      grid-template-columns: repeat(4, minmax(104px, 1fr));
      gap: 1px;
      width: min(690px, calc(100% - 36px));
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
      font-size: clamp(1.4rem, 3vw, 2.2rem);
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
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: clamp(2rem, 4vw, 3.4rem);
      line-height: 1.02;
      letter-spacing: 0;
    }
    .section-copy {
      margin: 0;
      color: var(--psy-muted);
      font-size: 1.05rem;
      line-height: 1.7;
    }
    .card-row { margin-top: 26px; }
    .psy-card mat-icon {
      margin-top: 18px;
      color: var(--psy-teal-deep);
    }
    .soft-band {
      background: linear-gradient(180deg, rgba(234,241,244,.62), rgba(244,247,250,.36));
    }
    .feature-band {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 360px;
      gap: 34px;
      align-items: center;
      padding: clamp(26px, 5vw, 54px);
      border-radius: 20px;
    }
    .feature-list {
      display: grid;
      gap: 12px;
    }
    .feature-list span,
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
    .feature-list mat-icon,
    .support-item mat-icon {
      color: var(--psy-green-deep);
      flex: 0 0 auto;
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
    .editorial-card {
      min-height: 250px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .editorial-card h3 {
      font-family: 'Cormorant Garamond', serif;
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
      min-width: 280px;
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
      .feature-band,
      .support-layout,
      .contact-card {
        grid-template-columns: 1fr;
      }
      .contact-actions {
        min-width: 0;
      }
    }
    @media (max-width: 520px) {
      .psy-public-nav {
        top: 10px;
        width: calc(100% - 20px);
      }
      .brand small { display: none; }
      .hero {
        padding-top: 122px;
        background-position: 58% center;
      }
      .metrics {
        grid-template-columns: 1fr 1fr;
      }
      .metric {
        padding: 14px 12px;
      }
      .hero-actions .psy-button {
        width: 100%;
      }
      .next-cue {
        display: none;
      }
    }
  `]
})
export class LandingComponent {
  menuOpen = signal(false);

  metrics = [
    { value: '25+', label: 'años de trayectoria' },
    { value: '12', label: 'semilleros activos' },
    { value: '38', label: 'convenios de práctica' },
    { value: '4.800+', label: 'egresados' }
  ];

  programs: SectionCard[] = [
    {
      icon: 'psychology',
      title: 'Pregrado en Psicología',
      text: 'Formación integral en fundamentos científicos, evaluación, intervención y responsabilidad social.',
      tag: 'Pregrado'
    },
    {
      icon: 'clinical_notes',
      title: 'Práctica profesional supervisada',
      text: 'Escenarios de aprendizaje auténtico con rutas de cuidado, evaluación formativa y acompañamiento docente.',
      tag: 'Práctica'
    },
    {
      icon: 'hub',
      title: 'Educación continua',
      text: 'Diplomados y cursos para fortalecer competencias en crisis, comunidad, bienestar y ética aplicada.',
      tag: 'Extensión'
    }
  ];

  researchLines: SectionCard[] = [
    { icon: 'monitoring', title: 'Salud mental', text: 'Bienestar, prevención y cuidado en contextos educativos.', tag: '' },
    { icon: 'diversity_3', title: 'Psicología social', text: 'Territorio, comunidad, convivencia y derechos humanos.', tag: '' },
    { icon: 'science', title: 'Métodos y datos', text: 'Diseños de investigación, medición y analítica académica.', tag: '' },
    { icon: 'school', title: 'Aprendizaje', text: 'Simulación, evaluación auténtica y práctica deliberada.', tag: '' }
  ];

  supportRoutes = [
    'Orientación inicial y remisión institucional.',
    'Salida segura y recursos de apoyo en casos sensibles.',
    'Lenguaje ético, no estigmatizante y centrado en derechos.',
    'Acompañamiento para estudiantes, docentes y comunidad.'
  ];
}
