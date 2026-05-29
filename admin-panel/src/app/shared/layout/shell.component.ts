import { CommonModule } from '@angular/common';
import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { APP_BRAND } from '../../core/config/brand.config';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  caption: string;
  roles: string[];
}

/** Returns true when the given URL is an active simulation play route. */
export function isGameRoute(url: string): boolean {
  return /\/portal\/simulador\/\d+/.test(url);
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatSidenavModule
  ],
  template: `
    <mat-sidenav-container class="portal-shell">
      <mat-sidenav #drawer class="portal-sidenav liquid-glass" [mode]="compactNav() ? 'over' : 'side'" [opened]="!compactNav() || drawerOpen()">
        <div class="sidenav-header">
          <a class="portal-brand" routerLink="/portal/dashboard" aria-label="Ir al dashboard">
            <img class="portal-brand__logo" src="/assets/images/institution/logo-cue-ccaq-vertical.webp" alt="CUE Alexander Von Humboldt" width="82" height="41">
            <span>
              <strong>{{ brand.shortName }}</strong>
              <small>{{ brand.fullName }}</small>
            </span>
          </a>
        </div>

        <mat-nav-list aria-label="Navegación del portal">
          @for (item of visibleNavItems(); track item.route) {
            <a mat-list-item [routerLink]="item.route" routerLinkActive="active-link" (click)="closeMobileNav()">
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
              <span matListItemLine>{{ item.caption }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content class="portal-content">
        <header class="portal-topbar liquid-glass">
          <button class="psy-icon-button" type="button" aria-label="Alternar navegación" (click)="toggleNav()">
            <mat-icon>menu</mat-icon>
          </button>
          <div>
            <p class="topbar-kicker">{{ brand.shortName }} · Portal académico</p>
            <h1>{{ currentSection() }}</h1>
          </div>
          <span class="topbar-spacer"></span>
          <div class="user-pill">
            <mat-icon>account_circle</mat-icon>
            <span>{{ currentUserLabel() }}</span>
          </div>
          <button class="psy-icon-button" type="button" aria-label="Cerrar sesión" (click)="auth.logout()">
            <mat-icon>logout</mat-icon>
          </button>
        </header>

        <main class="portal-main">
          <router-outlet />
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .portal-shell {
      min-height: 100vh;
      background: transparent;
    }
    .portal-sidenav {
      width: 296px;
      border-right: 1px solid var(--psy-border);
      border-radius: 0 22px 22px 0;
      background: rgba(255,255,255,.76);
      padding: 16px 12px;
    }
    .sidenav-header {
      padding: 10px 8px 18px;
      margin-bottom: 8px;
      border-bottom: 1px solid var(--psy-border);
    }
    .portal-brand {
      display: inline-flex;
      align-items: center;
      gap: 12px;
      min-height: 54px;
      color: var(--psy-ink);
    }
    .portal-brand__logo {
      width: 82px;
      height: auto;
      object-fit: contain;
      padding: 6px;
      border-radius: 14px;
      background: rgba(255,255,255,.78);
      border: 1px solid var(--psy-border);
    }
    .portal-brand strong,
    .portal-brand small { display: block; line-height: 1.12; }
    .portal-brand strong { font-size: 1.12rem; }
    .portal-brand small { color: var(--psy-muted); font-size: .78rem; margin-top: 3px; }
    mat-nav-list { display: grid; gap: 6px; }
    a[mat-list-item] {
      min-height: 58px;
      border-radius: 14px;
      color: var(--psy-ink);
    }
    a[mat-list-item] mat-icon { color: var(--psy-blue-deep); }
    .active-link {
      background: rgba(79,124,172,.12) !important;
      border: 1px solid rgba(79,124,172,.16);
    }
    .portal-content { min-height: 100vh; }
    .portal-topbar {
      position: sticky;
      z-index: 20;
      top: 16px;
      width: calc(100% - 32px);
      min-height: 72px;
      display: flex;
      align-items: center;
      gap: 14px;
      margin: 16px;
      padding: 10px 14px;
      border-radius: 20px;
    }
    .topbar-kicker {
      margin: 0;
      color: var(--siep-blue-soft);
      font-size: .72rem;
      font-weight: 800;
      letter-spacing: .12em;
      text-transform: uppercase;
    }
    .portal-topbar h1 {
      margin: 2px 0 0;
      color: var(--siep-blue);
      font-size: 1.25rem;
      font-weight: 700;
      line-height: 1.2;
    }
    .topbar-spacer { flex: 1; }
    .user-pill {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      max-width: 280px;
      min-height: 44px;
      padding: 0 14px;
      border-radius: 999px;
      color: var(--psy-muted);
      background: rgba(255,255,255,.52);
      border: 1px solid var(--psy-border);
      overflow: hidden;
    }
    .user-pill span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .portal-main { padding: 10px clamp(16px, 3vw, 36px) 44px; }
    @media (min-width: 921px) {
      .portal-topbar .psy-icon-button:first-child { display: none; }
    }
    @media (max-width: 920px) {
      .portal-sidenav { width: min(310px, 88vw); border-radius: 0 20px 20px 0; }
      .portal-topbar { top: 10px; width: calc(100% - 20px); margin: 10px; }
      .user-pill { display: none; }
      .portal-main { padding-inline: 16px; }
    }
    @media (max-width: 520px) {
      .portal-topbar h1 { font-size: 1rem; }
      .topbar-kicker { font-size: .64rem; }
    }
  `]
})
export class ShellComponent implements OnDestroy {
  readonly brand = APP_BRAND;
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly drawerOpen = signal(false);
  readonly compactNav = signal(window.matchMedia('(max-width: 920px)').matches);
  readonly inGameMode = signal(false);

  readonly navItems: NavItem[] = [
    { label: 'Dashboard',  icon: 'dashboard',    route: '/portal/dashboard',             caption: 'Seguimiento formativo', roles: ['ESTUDIANTE', 'PROFESOR', 'ADMIN'] },
    { label: 'Simulador',  icon: 'play_circle',  route: '/portal/simulador',             caption: 'Simulación formativa', roles: ['ESTUDIANTE', 'PROFESOR', 'ADMIN'] },
    { label: 'Casos',      icon: 'account_tree', route: '/portal/casos',                 caption: 'Catálogo y versiones', roles: ['PROFESOR', 'ADMIN'] },
    { label: 'Docente',    icon: 'timeline',     route: '/portal/docente/trazabilidad',  caption: 'Trazabilidad y rúbricas', roles: ['PROFESOR', 'ADMIN'] },
    { label: 'Grupos',     icon: 'groups',       route: '/portal/grupos',                caption: 'Cohortes académicas', roles: ['PROFESOR', 'ADMIN'] },
    { label: 'Reportes',   icon: 'analytics',    route: '/portal/reportes',              caption: 'Evaluación por rúbricas', roles: ['PROFESOR', 'ADMIN'] }
  ];

  readonly visibleNavItems = computed(() => {
    const role = this.auth.currentUser()?.role;
    if (!role) return this.navItems;
    return this.navItems.filter(item => item.roles.includes(role));
  });

  private readonly routerSub: Subscription;

  constructor() {
    window.matchMedia('(max-width: 920px)').addEventListener('change', event => {
      this.compactNav.set(event.matches);
      if (!event.matches) this.drawerOpen.set(false);
    });

    this.routerSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: NavigationEnd) => {
        const gameMode = isGameRoute(e.urlAfterRedirects);
        this.inGameMode.set(gameMode);
        if (gameMode) {
          document.body.classList.add('game-mode');
        } else {
          document.body.classList.remove('game-mode');
        }
      });
  }

  ngOnDestroy(): void {
    this.routerSub.unsubscribe();
    document.body.classList.remove('game-mode');
  }

  toggleNav() { this.drawerOpen.set(!this.drawerOpen()); }

  closeMobileNav() {
    if (this.compactNav()) this.drawerOpen.set(false);
  }

  currentUserLabel() {
    return this.auth.currentUser()?.role ? 'Cuenta institucional' : 'Usuario institucional';
  }

  currentSection() {
    return this.brand.subtitle;
  }
}
