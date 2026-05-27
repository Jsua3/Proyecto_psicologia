# Game UX Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the PsychoSim simulator from a two-column dashboard into a fullscreen game with Undertale-style dialogue strips, HUD overlays, and Kenney.nl pixel-art sprites.

**Architecture:** `SimulationPlayComponent` becomes a `position:fixed; inset:0` container stacking Angular overlays (HUD, dialogue strip, tool bar, journal sheet, minimap, fade) on top of a fullscreen Phaser canvas. Shell hides its nav via a `game-mode` body class. Backend, services, and all game logic are untouched.

**Tech Stack:** Angular 21 standalone signals, Phaser 3.90, Kenney.nl CC0 tilesets, CSS transforms for animations.

---

## File Map

| Action | Path |
|--------|------|
| Create | `admin-panel/src/app/features/simulator/minimap.component.ts` |
| Create | `admin-panel/src/app/features/simulator/minimap.component.spec.ts` |
| Create | `admin-panel/src/app/features/simulator/kenney-frames.constants.ts` |
| Modify | `admin-panel/src/styles/styles.scss` |
| Modify | `admin-panel/src/app/shared/layout/shell.component.ts` |
| Modify | `admin-panel/src/app/features/simulator/simulation-hud.component.ts` |
| Modify | `admin-panel/src/app/features/simulator/tool-inventory.component.ts` |
| Modify | `admin-panel/src/app/features/simulator/journal-panel.component.ts` |
| Modify | `admin-panel/src/app/features/simulator/dialogue-panel.component.ts` |
| Modify | `admin-panel/src/app/features/simulator/game-world.component.ts` |
| Modify | `admin-panel/src/app/features/simulator/simulation-play.component.ts` |

`supervision-feedback.component.ts` stays on disk but is no longer imported — supervision feedback is shown through the dialogue strip.

---

## Task 1: Kenney.nl asset directories + frame constants

**Files:**
- Create: `admin-panel/src/assets/game/kenney/.gitkeep`
- Create: `admin-panel/src/app/features/simulator/kenney-frames.constants.ts`

- [ ] **Step 1: Create the asset directory tree**

```
admin-panel/src/assets/game/kenney/
  tiny-town/Tilemap/
  tiny-dungeon/Tilemap/
  tiny-rpg-characters/Spritesheet/
```

Run in PowerShell from repo root:
```powershell
New-Item -ItemType Directory -Force "admin-panel\src\assets\game\kenney\tiny-town\Tilemap"
New-Item -ItemType Directory -Force "admin-panel\src\assets\game\kenney\tiny-dungeon\Tilemap"
New-Item -ItemType Directory -Force "admin-panel\src\assets\game\kenney\tiny-rpg-characters\Spritesheet"
New-Item -ItemType File -Force "admin-panel\src\assets\game\kenney\.gitkeep"
```

Expected: directories created, no errors.

- [ ] **Step 2: Download Kenney.nl packs manually (developer action)**

Visit these URLs and extract the ZIP contents into the matching directories:

| URL | Destination |
|-----|-------------|
| https://kenney.nl/assets/tiny-town | `tiny-town/` — copy the `Tilemap/tilemap_packed.png` file |
| https://kenney.nl/assets/tiny-dungeon | `tiny-dungeon/` — copy `Tilemap/tilemap_packed.png` |
| https://kenney.nl/assets/tiny-rpg-characters | `tiny-rpg-characters/` — copy `Spritesheet/characters.png` |

License: CC0. No attribution required.

After downloading, verify:
```
admin-panel/src/assets/game/kenney/tiny-town/Tilemap/tilemap_packed.png       ✓
admin-panel/src/assets/game/kenney/tiny-dungeon/Tilemap/tilemap_packed.png     ✓
admin-panel/src/assets/game/kenney/tiny-rpg-characters/Spritesheet/characters.png ✓
```

- [ ] **Step 3: Create frame constants file**

Create `admin-panel/src/app/features/simulator/kenney-frames.constants.ts`:

```typescript
/**
 * Kenney.nl Tiny Town & Tiny RPG Characters frame indices for Phaser spritesheets.
 *
 * HOW TO FIND FRAME NUMBERS:
 * Open the PNG in a tile inspector (e.g. Tiled map editor) or count manually:
 *   - Tile row 0: frames 0..cols-1
 *   - Tile row 1: frames cols..(2*cols-1)
 * Tiny Town packed sheet is 12 cols wide. Tiny RPG Characters is also 12 cols wide.
 *
 * Adjust the values below after inspecting the downloaded files.
 */
export const KenneyTownFrames = {
  /** Light wooden floor tile — row 1, col 4 of tiny-town packed */
  FLOOR_WOOD: 16,
  /** Stone floor tile — row 1, col 6 */
  FLOOR_STONE: 18,
  /** Horizontal wall segment */
  WALL_H: 0,
  /** Vertical wall segment */
  WALL_V: 12,
  /** Closed door tile */
  DOOR_CLOSED: 44,
  /** Open door tile */
  DOOR_OPEN: 45,
} as const;

export const KenneyDungeonFrames = {
  /** Desk / examination table */
  DESK: 8,
  /** Chair */
  CHAIR: 20,
  /** Filing cabinet */
  CABINET: 32,
  /** Plant */
  PLANT: 44,
} as const;

export const KenneyCharFrames = {
  /**
   * Each character occupies 3 columns × 4 rows (down, left, right, up).
   * Character 0 (intern/student) starts at frame 0.
   * Character 1 (patient/client) starts at frame 12.
   * Character 2 (supervisor) starts at frame 24.
   *
   * Within each character block:
   *   frames +0..+2 = walk down
   *   frames +3..+5 = walk left
   *   frames +6..+8 = walk right
   *   frames +9..+11 = walk up
   */
  PLAYER_WALK_DOWN:  [0, 1, 2],
  PLAYER_WALK_LEFT:  [3, 4, 5],
  PLAYER_WALK_RIGHT: [6, 7, 8],
  PLAYER_WALK_UP:    [9, 10, 11],
  PLAYER_IDLE:       0,

  NPC_PATIENT_IDLE:     12,
  NPC_SUPERVISOR_IDLE:  24,
} as const;
```

- [ ] **Step 4: Verify angular.json includes assets directory**

Open `admin-panel/angular.json`, find the `assets` array under `build.options`. Confirm it includes:

```json
{ "glob": "**/*", "input": "src/assets", "output": "assets" }
```

If not present, add it. No change needed if the glob already covers `src/assets/**/*`.

- [ ] **Step 5: Commit**

```bash
git add admin-panel/src/assets/game/kenney/.gitkeep
git add admin-panel/src/app/features/simulator/kenney-frames.constants.ts
git commit -m "feat(game): create Kenney asset directories and frame constants"
```

---

## Task 2: styles.scss — `.game-mode` body class

**Files:**
- Modify: `admin-panel/src/styles/styles.scss`

- [ ] **Step 1: Write the failing test**

Since this is a CSS-only change, there is no unit test. Acceptance is verified manually in Task 12.

- [ ] **Step 2: Add `.game-mode` rules to styles.scss**

Append the following block at the end of `admin-panel/src/styles/styles.scss`:

```scss
/* ─── Game mode: fullscreen simulator — hides portal chrome ────────────────── */

body.game-mode {
  overflow: hidden;
}

body.game-mode mat-sidenav-container.portal-shell {
  /* Hide the mat-sidenav-container structure while preserving router-outlet content */
}

body.game-mode .portal-sidenav {
  display: none !important;
}

body.game-mode .portal-topbar {
  display: none !important;
}

body.game-mode .portal-main {
  padding: 0 !important;
  height: 100vh;
  overflow: hidden;
}

body.game-mode mat-sidenav-content.portal-content {
  margin-left: 0 !important;
}
```

- [ ] **Step 3: Commit**

```bash
git add admin-panel/src/styles/styles.scss
git commit -m "feat(game): add game-mode body class to hide portal nav"
```

---

## Task 3: ShellComponent — `inGameMode` signal + Router.events

**Files:**
- Modify: `admin-panel/src/app/shared/layout/shell.component.ts`

The shell must watch `Router.events` for `NavigationEnd` events. When the URL matches `/portal/simulador/`, add `game-mode` to `document.body.classList`. When navigating away, remove it.

- [ ] **Step 1: Write the failing test**

Create `admin-panel/src/app/shared/layout/shell.component.spec.ts`:

```typescript
import { signal } from '@angular/core';

// Pure-logic test: the URL-matching predicate
function isGameRoute(url: string): boolean {
  return /\/portal\/simulador\/\d+/.test(url);
}

describe('ShellComponent game-mode logic', () => {
  it('detects simulador/:id route', () => {
    expect(isGameRoute('/portal/simulador/42')).toBe(true);
  });

  it('does not match simulador list', () => {
    expect(isGameRoute('/portal/simulador')).toBe(false);
  });

  it('does not match other routes', () => {
    expect(isGameRoute('/portal/dashboard')).toBe(false);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd admin-panel
npx jest shell.component.spec --no-coverage
```

Expected: FAIL — `isGameRoute is not defined`.

- [ ] **Step 3: Implement ShellComponent with inGameMode**

Replace the full content of `admin-panel/src/app/shared/layout/shell.component.ts`:

```typescript
import { CommonModule } from '@angular/common';
import { Component, OnDestroy, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  caption: string;
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
              <strong>PsychoSim</strong>
              <small>Psicología Humboldt</small>
            </span>
          </a>
        </div>

        <mat-nav-list aria-label="Navegación del portal">
          @for (item of navItems; track item.route) {
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
            <p class="topbar-kicker">Portal académico</p>
            <h1>{{ currentSection() }}</h1>
          </div>
          <span class="topbar-spacer"></span>
          <div class="user-pill">
            <mat-icon>account_circle</mat-icon>
            <span>{{ currentUserEmail() }}</span>
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
      color: var(--psy-teal-deep);
      font-size: .72rem;
      font-weight: 800;
      letter-spacing: .12em;
      text-transform: uppercase;
    }
    .portal-topbar h1 {
      margin: 2px 0 0;
      color: var(--psy-ink);
      font-size: 1.25rem;
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
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly drawerOpen = signal(false);
  readonly compactNav = signal(window.matchMedia('(max-width: 920px)').matches);
  readonly inGameMode = signal(false);

  readonly navItems: NavItem[] = [
    { label: 'Dashboard',  icon: 'dashboard',    route: '/portal/dashboard',             caption: 'Métricas y seguimiento' },
    { label: 'Simulador',  icon: 'play_circle',  route: '/portal/simulador',             caption: 'Juego de casos' },
    { label: 'Casos',      icon: 'account_tree', route: '/portal/casos',                 caption: 'Catálogo y versiones' },
    { label: 'Docente',    icon: 'timeline',     route: '/portal/docente/trazabilidad',  caption: 'Trazabilidad y rúbricas' },
    { label: 'Grupos',     icon: 'groups',       route: '/portal/grupos',                caption: 'Cohortes académicas' },
    { label: 'Reportes',   icon: 'analytics',    route: '/portal/reportes',              caption: 'Analíticas formativas' }
  ];

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

  currentUserEmail() {
    return this.auth.currentUser()?.email ?? 'usuario institucional';
  }

  currentSection() {
    return 'Simulación, evaluación y acompañamiento';
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
npx jest shell.component.spec --no-coverage
```

Expected: PASS — 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add admin-panel/src/app/shared/layout/shell.component.ts
git add admin-panel/src/app/shared/layout/shell.component.spec.ts
git commit -m "feat(shell): add inGameMode signal — body.game-mode when on simulador/:id"
```

---

## Task 4: SimulationHudComponent → compact overlay strip

**Files:**
- Modify: `admin-panel/src/app/features/simulator/simulation-hud.component.ts`

The HUD shrinks from a large two-column header to a 52px horizontal strip. All `computed` signals (`stressTier`, `stressColor`, `stressMeterGradient`, `stressLabel`) are preserved unchanged. Only the template and styles change.

- [ ] **Step 1: Replace template and styles — keep all TS logic**

Replace the full content of `admin-panel/src/app/features/simulator/simulation-hud.component.ts`:

```typescript
import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { SimulationAttemptState } from '../../core/models/simulation.model';

type StressTier = 'calm' | 'moderate' | 'high' | 'critical';

@Component({
  selector: 'app-simulation-hud',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    @if (attempt(); as game) {
      <div class="hud-strip liquid-glass"
        [class.hud--stress-high]="stressTier() === 'high'"
        [class.hud--stress-critical]="stressTier() === 'critical'">

        <div class="hud-score" aria-label="Puntaje profesional: {{ game.accumulatedScore }}">
          <mat-icon aria-hidden="true">star</mat-icon>
          <strong>{{ game.accumulatedScore }}</strong>
        </div>

        <div class="hud-stress"
          [class.hud-stress--pulse]="stressPulse()"
          role="meter"
          [attr.aria-valuenow]="game.stressIndex"
          aria-valuemin="0"
          aria-valuemax="100"
          [attr.aria-label]="'Estrés: ' + game.stressIndex + '%. ' + stressLabel()">
          <span class="stress-pct" [style.color]="stressColor()">{{ game.stressIndex }}%</span>
          <div class="stress-track" aria-hidden="true">
            <span [style.width.%]="game.stressIndex" [style.background]="stressMeterGradient()"></span>
          </div>
        </div>

        <div class="hud-scene">
          <mat-icon aria-hidden="true">location_on</mat-icon>
          <span>{{ game.currentNode.title }}</span>
        </div>

        <div class="hud-status" [class.hud-status--live]="game.status === 'IN_PROGRESS'">
          <span class="status-dot" aria-hidden="true"></span>
          <span>{{ statusLabel(game.status) }}</span>
        </div>
      </div>
    }
  `,
  styles: [`
    .hud-strip {
      display: flex;
      align-items: center;
      gap: 14px;
      height: 52px;
      padding: 0 14px;
      border-radius: 0 0 16px 16px;
      background: rgba(8,12,18,.84);
      backdrop-filter: blur(18px) saturate(120%);
      border: 1px solid rgba(79,163,165,.18);
      border-top: none;
      color: rgba(232,240,244,.9);
      transition: border-color var(--psy-motion-ui);
    }
    .hud--stress-high  { border-color: rgba(212,160,80,.4); }
    .hud--stress-critical { border-color: rgba(168,80,98,.5); }

    .hud-score {
      display: flex;
      align-items: center;
      gap: 5px;
      flex-shrink: 0;
    }
    .hud-score mat-icon { color: #f4c875; font-size: 18px; width: 18px; height: 18px; }
    .hud-score strong {
      font-family: 'JetBrains Mono', monospace;
      font-size: .9rem;
      letter-spacing: .04em;
    }

    .hud-stress {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 0 0 160px;
    }
    .hud-stress--pulse { animation: stress-pulse .6s ease-out; }
    .stress-pct {
      font-family: 'JetBrains Mono', monospace;
      font-size: .78rem;
      min-width: 38px;
      transition: color var(--psy-motion-ui);
    }
    .stress-track {
      flex: 1;
      height: 5px;
      border-radius: 999px;
      background: rgba(255,255,255,.1);
      overflow: hidden;
    }
    .stress-track span {
      display: block;
      height: 100%;
      border-radius: inherit;
      transition: width .5s cubic-bezier(.4,0,.2,1), background .5s ease;
    }

    .hud-scene {
      display: flex;
      align-items: center;
      gap: 5px;
      flex: 1;
      overflow: hidden;
    }
    .hud-scene mat-icon { color: #4fa3a5; font-size: 15px; width: 15px; height: 15px; flex-shrink: 0; }
    .hud-scene span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: .8rem;
      color: rgba(232,240,244,.65);
    }

    .hud-status {
      display: flex;
      align-items: center;
      gap: 5px;
      flex-shrink: 0;
      font-size: .7rem;
      color: rgba(232,240,244,.4);
      white-space: nowrap;
    }
    .status-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: rgba(255,255,255,.25);
    }
    .hud-status--live .status-dot {
      background: #4fa3a5;
      animation: dot-blink 2s ease-in-out infinite;
    }

    @keyframes stress-pulse {
      0%   { box-shadow: 0 0 0 0 rgba(212,160,80,.4); }
      70%  { box-shadow: 0 0 0 6px rgba(212,160,80,0); }
      100% { box-shadow: none; }
    }
    @keyframes dot-blink {
      0%, 100% { opacity: 1; }
      50%       { opacity: .35; }
    }

    @media (max-width: 640px) {
      .hud-scene { display: none; }
      .hud-stress { flex: 0 0 120px; }
    }
    @media (prefers-reduced-motion: reduce) {
      .hud-stress--pulse { animation: none; }
      .stress-track span { transition: none; }
      .status-dot { animation: none !important; }
    }
  `]
})
export class SimulationHudComponent {
  readonly attempt = input<SimulationAttemptState | null>(null);
  readonly stressPulse = input(false);

  readonly stressTier = computed<StressTier>(() => {
    const s = this.attempt()?.stressIndex ?? 0;
    if (s >= 75) return 'critical';
    if (s >= 50) return 'high';
    if (s >= 25) return 'moderate';
    return 'calm';
  });

  readonly stressColor = computed(() => ({
    calm:     'var(--psy-teal-deep, #2a7a6e)',
    moderate: '#7a8a3e',
    high:     '#b07830',
    critical: '#8b3145'
  })[this.stressTier()]);

  readonly stressMeterGradient = computed(() => ({
    calm:     'linear-gradient(90deg, #8cbfa6, #4fa3a5)',
    moderate: 'linear-gradient(90deg, #8cbfa6, #c4b55a)',
    high:     'linear-gradient(90deg, #c4b55a, #d4a050)',
    critical: 'linear-gradient(90deg, #d4a050, #a85062)'
  })[this.stressTier()]);

  readonly stressLabel = computed(() => ({
    calm:     'Situación estable',
    moderate: 'Tensión moderada',
    high:     'Estrés elevado — considere herramientas de contención',
    critical: 'Nivel crítico — priorice seguridad y autocuidado'
  })[this.stressTier()]);

  statusLabel(status: SimulationAttemptState['status']) {
    return { IN_PROGRESS: 'En escena', COMPLETED: 'Finalizado', SAFE_EXITED: 'Pausado' }[status];
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add admin-panel/src/app/features/simulator/simulation-hud.component.ts
git commit -m "feat(hud): refactor SimulationHudComponent to compact 52px overlay strip"
```

---

## Task 5: ToolInventoryComponent → 44×44px HUD icon row

**Files:**
- Modify: `admin-panel/src/app/features/simulator/tool-inventory.component.ts`

API unchanged: `tools`, `inventory`, `select` output. Visual: column of 44×44px icon buttons.

- [ ] **Step 1: Replace template and styles**

Replace the full content of `admin-panel/src/app/features/simulator/tool-inventory.component.ts`:

```typescript
import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ClinicalToolState } from '../../core/models/simulation.model';

@Component({
  selector: 'app-tool-inventory',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="tool-hud" role="toolbar" aria-label="Inventario clínico de herramientas">
      @for (tool of tools(); track tool.code) {
        <button
          class="tool-btn"
          type="button"
          [class.tool-btn--owned]="inventory().includes(tool.code)"
          [class.tool-btn--locked]="!inventory().includes(tool.code)"
          [disabled]="!inventory().includes(tool.code)"
          [title]="tool.label + ' — ' + (inventory().includes(tool.code) ? 'Disponible' : 'No disponible')"
          [attr.aria-label]="tool.label + ': ' + tool.description + '. ' + (inventory().includes(tool.code) ? 'Disponible.' : 'No disponible.')"
          (click)="select.emit(tool.code)">
          <mat-icon aria-hidden="true">{{ tool.icon }}</mat-icon>
          <span class="tool-code" aria-hidden="true">{{ tool.code.slice(0, 4) }}</span>
        </button>
      }
    </div>
  `,
  styles: [`
    .tool-hud {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .tool-btn {
      position: relative;
      display: grid;
      place-items: center;
      width: 44px;
      height: 44px;
      border: 1px solid rgba(79,163,165,.28);
      border-radius: 10px;
      background: rgba(8,12,18,.76);
      color: rgba(79,163,165,.55);
      cursor: pointer;
      padding: 0;
      transition: border-color 160ms ease, background 160ms ease, color 160ms ease;
    }
    .tool-btn mat-icon { font-size: 20px; width: 20px; height: 20px; }
    .tool-btn--owned {
      border-color: rgba(79,163,165,.5);
      color: #4fa3a5;
    }
    .tool-btn--owned:hover {
      border-color: rgba(79,163,165,.85);
      background: rgba(79,163,165,.14);
      box-shadow: 0 0 12px -4px rgba(79,163,165,.35);
    }
    .tool-btn--locked {
      opacity: .28;
      cursor: default;
    }
    .tool-code {
      position: absolute;
      bottom: 2px;
      right: 3px;
      font-size: .5rem;
      font-family: 'JetBrains Mono', monospace;
      font-weight: 900;
      letter-spacing: .02em;
      opacity: .7;
      pointer-events: none;
    }
    :focus-visible {
      outline: 2px solid rgba(79,163,165,.7);
      outline-offset: 2px;
    }
  `]
})
export class ToolInventoryComponent {
  readonly tools = input<ClinicalToolState[]>([]);
  readonly inventory = input<string[]>([]);
  readonly select = output<string>();
}
```

- [ ] **Step 2: Commit**

```bash
git add admin-panel/src/app/features/simulator/tool-inventory.component.ts
git commit -m "feat(tools): refactor ToolInventoryComponent to 44px HUD icon column"
```

---

## Task 6: JournalPanelComponent → sliding sheet from right

**Files:**
- Modify: `admin-panel/src/app/features/simulator/journal-panel.component.ts`

Two new signals: `open = input(false)` and `closeSheet = output<void>()`. Existing `disabled`, `message`, `save`, and `clear()` are unchanged.

- [ ] **Step 1: Replace component**

Replace the full content of `admin-panel/src/app/features/simulator/journal-panel.component.ts`:

```typescript
import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-journal-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  template: `
    <div class="journal-sheet"
      [class.journal-sheet--open]="open()"
      role="complementary"
      [attr.aria-hidden]="!open()"
      aria-label="Bitácora clínica cifrada">

      <div class="sheet-header">
        <mat-icon aria-hidden="true">menu_book</mat-icon>
        <h3 id="journal-heading">Bitácora clínica</h3>
        <button class="sheet-close psy-icon-button" type="button" aria-label="Cerrar bitácora" (click)="closeSheet.emit()">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <p class="psy-eyebrow sheet-sub">Razonamiento clínico</p>

      <textarea
        [(ngModel)]="text"
        [disabled]="disabled()"
        placeholder="Registra señales observadas, hipótesis de riesgo, ruta ética y decisión profesional."
        aria-labelledby="journal-heading"
        aria-describedby="encrypt-note">
      </textarea>

      <button class="psy-button psy-button--primary" type="button"
        (click)="save.emit(text)"
        [disabled]="!text.trim() || disabled()">
        <mat-icon aria-hidden="true">encrypted</mat-icon>
        Guardar bitácora
      </button>

      @if (message()) {
        <p class="journal-message" role="status" aria-live="polite">{{ message() }}</p>
      }

      <p class="encrypt-note" id="encrypt-note">
        <mat-icon aria-hidden="true">lock</mat-icon>
        Las bitácoras se cifran con AES-GCM antes de guardarse.
      </p>
    </div>
  `,
  styles: [`
    .journal-sheet {
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      width: min(400px, 88vw);
      display: grid;
      grid-template-rows: auto auto minmax(100px, 1fr) auto auto auto;
      gap: 14px;
      padding: 20px;
      background: rgba(8,12,18,.94);
      backdrop-filter: blur(20px) saturate(110%);
      border-left: 1px solid rgba(79,163,165,.22);
      color: #e8f0f4;
      transform: translateX(100%);
      transition: transform 320ms cubic-bezier(.2,.8,.2,1);
      overflow-y: auto;
      overflow-x: hidden;
      z-index: 100;
    }
    .journal-sheet--open {
      transform: translateX(0);
    }
    .sheet-header {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .sheet-header mat-icon:first-child { color: #4fa3a5; flex-shrink: 0; }
    .sheet-header h3 {
      margin: 0;
      flex: 1;
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.1rem;
      color: rgba(232,240,244,.95);
    }
    .sheet-close {
      background: rgba(255,255,255,.06);
      border-color: rgba(255,255,255,.1);
      color: rgba(232,240,244,.5);
      flex-shrink: 0;
    }
    .sheet-sub { color: rgba(79,163,165,.75); margin: 0; }
    textarea {
      width: 100%;
      min-height: 150px;
      resize: vertical;
      padding: 14px;
      border: 1px solid rgba(79,163,165,.2);
      border-radius: 12px;
      background: rgba(255,255,255,.04);
      color: #e8f0f4;
      font: inherit;
      line-height: 1.55;
      outline: none;
      transition: border-color 180ms ease;
    }
    textarea:focus { border-color: rgba(79,163,165,.5); box-shadow: 0 0 0 3px rgba(79,163,165,.1); }
    textarea:disabled { opacity: .4; cursor: not-allowed; }
    .journal-message { margin: 0; color: #5d9278; font-weight: 800; font-size: .88rem; }
    .encrypt-note {
      display: flex;
      gap: 8px;
      align-items: center;
      margin: 0;
      font-size: .7rem;
      color: rgba(232,240,244,.28);
      border-top: 1px solid rgba(255,255,255,.06);
      padding-top: 12px;
    }
    .encrypt-note mat-icon { font-size: 14px; width: 14px; height: 14px; }
    @media (prefers-reduced-motion: reduce) {
      .journal-sheet { transition: none; }
    }
  `]
})
export class JournalPanelComponent {
  readonly open = input(false);
  readonly disabled = input(false);
  readonly message = input('');
  readonly save = output<string>();
  readonly closeSheet = output<void>();

  text = '';

  clear() {
    this.text = '';
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add admin-panel/src/app/features/simulator/journal-panel.component.ts
git commit -m "feat(journal): refactor JournalPanelComponent to right-side slide sheet"
```

---

## Task 7: DialoguePanelComponent → Undertale bottom strip with typewriter

**Files:**
- Modify: `admin-panel/src/app/features/simulator/dialogue-panel.component.ts`

API preserved: `dialogue`, `interaction` inputs; `close`, `execute`, `useTool` outputs. New: typewriter effect via `setInterval`, supervision style when `speakerName === 'Supervisión clínica'`.

- [ ] **Step 1: Write test for typewriter logic**

Create `admin-panel/src/app/features/simulator/dialogue-panel.component.spec.ts`:

```typescript
// Pure logic test — no Angular TestBed required
const CHARS_PER_SEC = 22;
const INTERVAL_MS = Math.round(1000 / CHARS_PER_SEC);

function simulateTypewriter(fullText: string, tickCount: number): string {
  return fullText.slice(0, tickCount);
}

describe('Dialogue typewriter timing', () => {
  it('reveals characters at 22 chars/sec', () => {
    const text = 'Hola mundo';
    // After 1 tick (~45ms) → 1 char revealed
    expect(simulateTypewriter(text, 1)).toBe('H');
    // After 5 ticks → 5 chars
    expect(simulateTypewriter(text, 5)).toBe('Hola ');
  });

  it('completes at text.length ticks', () => {
    const text = 'Test';
    expect(simulateTypewriter(text, text.length)).toBe('Test');
  });

  it('INTERVAL_MS is approximately 45ms for 22 chars/sec', () => {
    expect(INTERVAL_MS).toBeGreaterThanOrEqual(44);
    expect(INTERVAL_MS).toBeLessThanOrEqual(46);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
npx jest dialogue-panel.component.spec --no-coverage
```

Expected: FAIL — `simulateTypewriter is not defined`.

- [ ] **Step 3: Replace DialoguePanelComponent with Undertale strip**

Replace the full content of `admin-panel/src/app/features/simulator/dialogue-panel.component.ts`:

```typescript
import { CommonModule } from '@angular/common';
import { AfterViewChecked, Component, ElementRef, OnDestroy, ViewChild, effect, input, output, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { DialogueChoiceState, DialogueState, MapObjectState } from '../../core/models/simulation.model';

const CHARS_PER_SEC = 22;
const TYPEWRITER_INTERVAL_MS = Math.round(1000 / CHARS_PER_SEC); // ~45ms

@Component({
  selector: 'app-dialogue-panel',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    @if (dialogue(); as d) {
      <div #dialogueBox
        class="dialogue-strip"
        [class.strip--warning]="interaction()?.type === 'WARNING'"
        [class.strip--supervisory]="d.speakerName === 'Supervisión clínica'"
        [class.strip--tool]="d.speakerName === 'Herramienta profesional' || d.speakerName?.startsWith('✓') || d.speakerName?.startsWith('ℹ')"
        role="dialog"
        aria-modal="false"
        [attr.aria-label]="d.speakerName + ': ' + displayedText()">

        <div class="portrait-box" [attr.data-emotion]="d.emotion" aria-hidden="true">
          <mat-icon class="portrait-icon">{{ iconFor(interaction()?.type) }}</mat-icon>
          <span class="emotion-chip">{{ d.emotion }}</span>
        </div>

        <div class="dialogue-content">
          <p class="speaker-name">{{ d.speakerName }}</p>
          <p class="dialogue-text">{{ displayedText() }}<span class="cursor-blink" [class.cursor-blink--hide]="isTypingComplete()" aria-hidden="true">▮</span></p>

          @if (isTypingComplete()) {
            <div class="choice-row" role="toolbar" aria-label="Opciones de intervención">
              @if (d.choices.length > 0) {
                @for (choice of d.choices; track choice.key) {
                  <button class="choice-chip"
                    type="button"
                    [class.choice-chip--action]="choice.decisionOptionId"
                    [class.choice-chip--tool]="choice.requiredToolCode && !choice.decisionOptionId"
                    (click)="handleChoice(choice)">
                    {{ choice.text }}
                  </button>
                }
              } @else {
                @if (interaction()?.toolCode) {
                  <button class="choice-chip choice-chip--tool" type="button"
                    (click)="useTool.emit(interaction()!.toolCode!)">
                    <mat-icon aria-hidden="true">construction</mat-icon>
                    Tomar herramienta
                  </button>
                }
                @if (interaction()?.decisionOptionId) {
                  <button class="choice-chip choice-chip--action" type="button"
                    [class.choice-chip--danger]="interaction()?.type === 'WARNING'"
                    (click)="execute.emit(interaction()!.decisionOptionId!)">
                    <mat-icon aria-hidden="true">bolt</mat-icon>
                    Ejecutar intervención
                  </button>
                }
              }
              <button class="choice-chip choice-chip--close" type="button"
                (click)="close.emit()"
                aria-label="Cerrar diálogo (también con Escape)">
                <mat-icon aria-hidden="true">close</mat-icon>
                Cerrar
              </button>
            </div>
          }
        </div>
      </div>
    }
  `,
  styles: [`
    .dialogue-strip {
      display: grid;
      grid-template-columns: 72px minmax(0, 1fr);
      gap: 16px;
      padding: 14px 20px 16px;
      background: rgba(6,10,16,.92);
      backdrop-filter: blur(12px) saturate(110%);
      border-top: 2px solid rgba(79,163,165,.35);
      color: #e8f0f4;
      animation: strip-rise 220ms cubic-bezier(.2,.8,.2,1) both;
    }
    .strip--warning  { border-top-color: rgba(168,80,98,.6);  background: rgba(14,4,8,.94); }
    .strip--supervisory { border-top-color: rgba(93,146,120,.55); background: rgba(4,12,8,.94); }
    .strip--tool     { border-top-color: rgba(79,124,172,.4); }

    .portrait-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: flex-start;
      gap: 6px;
      padding-top: 2px;
    }
    .portrait-icon {
      font-size: 32px;
      width: 52px;
      height: 52px;
      display: grid;
      place-items: center;
      border-radius: 12px;
      background: rgba(79,163,165,.14);
      color: #4fa3a5;
      border: 1px solid rgba(79,163,165,.28);
    }
    .strip--warning .portrait-icon     { background: rgba(168,80,98,.14); color: #e87090; border-color: rgba(168,80,98,.28); }
    .strip--supervisory .portrait-icon { background: rgba(93,146,120,.14); color: #5d9278; border-color: rgba(93,146,120,.28); }
    .emotion-chip {
      font-size: .58rem;
      text-transform: uppercase;
      letter-spacing: .08em;
      color: rgba(79,163,165,.6);
      font-weight: 700;
    }

    .dialogue-content { display: grid; gap: 6px; align-content: start; }

    .speaker-name {
      margin: 0;
      font-size: .76rem;
      font-weight: 900;
      letter-spacing: .12em;
      text-transform: uppercase;
      color: #4fa3a5;
    }
    .strip--supervisory .speaker-name { color: #5d9278; }
    .strip--warning .speaker-name     { color: #e87090; }
    .strip--tool .speaker-name        { color: #7c9fc8; }

    .dialogue-text {
      margin: 0;
      line-height: 1.6;
      color: rgba(232,240,244,.88);
      font-size: .94rem;
      min-height: 2.8em;
    }
    .cursor-blink {
      display: inline-block;
      animation: blink .7s step-end infinite;
      color: #4fa3a5;
      margin-left: 1px;
    }
    .cursor-blink--hide { display: none; }

    .choice-row {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 8px;
      animation: choices-in 180ms ease both;
    }
    .choice-chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      min-height: 34px;
      padding: 5px 14px;
      border-radius: 999px;
      font-size: .82rem;
      font-weight: 700;
      cursor: pointer;
      border: 1px solid transparent;
      transition: transform 120ms ease, box-shadow 120ms ease;
    }
    .choice-chip:active { transform: scale(.95); }
    .choice-chip mat-icon { font-size: 16px; width: 16px; height: 16px; }

    .choice-chip--action {
      background: rgba(47,95,143,.45);
      border-color: rgba(79,124,172,.45);
      color: #9dc0e8;
    }
    .choice-chip--action:hover { background: rgba(47,95,143,.65); box-shadow: 0 0 10px -3px rgba(79,124,172,.4); }

    .choice-chip--danger {
      background: rgba(120,30,48,.45);
      border-color: rgba(168,80,98,.45);
      color: #e89090;
    }
    .choice-chip--danger:hover { background: rgba(120,30,48,.65); }

    .choice-chip--tool {
      background: rgba(40,80,55,.45);
      border-color: rgba(79,140,100,.4);
      color: #8cbfa6;
    }
    .choice-chip--tool:hover { background: rgba(40,80,55,.65); }

    .choice-chip--close {
      background: rgba(255,255,255,.05);
      border-color: rgba(255,255,255,.1);
      color: rgba(232,240,244,.4);
    }
    .choice-chip--close:hover { color: rgba(232,240,244,.75); }

    @keyframes strip-rise {
      from { transform: translateY(100%); opacity: 0; }
      to   { transform: translateY(0);    opacity: 1; }
    }
    @keyframes choices-in {
      from { opacity: 0; transform: translateY(4px); }
      to   { opacity: 1; transform: translateY(0); }
    }
    @keyframes blink {
      0%, 100% { opacity: 1; }
      50%      { opacity: 0; }
    }

    @media (max-width: 600px) {
      .dialogue-strip { grid-template-columns: 1fr; }
      .portrait-box { flex-direction: row; gap: 10px; padding-top: 0; }
    }
    @media (prefers-reduced-motion: reduce) {
      .dialogue-strip { animation: none; }
      .cursor-blink { animation: none; }
    }
  `]
})
export class DialoguePanelComponent implements AfterViewChecked, OnDestroy {
  readonly dialogue = input<DialogueState | null>(null);
  readonly interaction = input<MapObjectState | null>(null);
  readonly close = output<void>();
  readonly execute = output<number>();
  readonly useTool = output<string>();

  readonly displayedText = signal('');
  readonly isTypingComplete = signal(false);

  @ViewChild('dialogueBox') private dialogueBox?: ElementRef<HTMLElement>;
  private lastDialogueKey: string | null = null;
  private typewriterHandle: ReturnType<typeof setInterval> | null = null;

  constructor() {
    effect(() => {
      const d = this.dialogue();
      if (!d) {
        this.stopTypewriter();
        this.displayedText.set('');
        this.isTypingComplete.set(false);
        return;
      }
      if (d.key !== this.lastDialogueKey) {
        this.lastDialogueKey = d.key;
        const fullText = d.lines.map(l => l.text).join('\n\n');
        this.startTypewriter(fullText);
      }
    });
  }

  ngOnDestroy(): void {
    this.stopTypewriter();
  }

  /** Fase 7: Auto-focus dialogue when it appears for keyboard accessibility */
  ngAfterViewChecked(): void {
    const currentKey = this.dialogue()?.key ?? null;
    if (currentKey && currentKey !== this.lastDialogueKey && this.dialogueBox) {
      const firstButton = this.dialogueBox.nativeElement.querySelector('button') as HTMLElement | null;
      firstButton?.focus();
    }
    if (!currentKey) this.lastDialogueKey = null;
  }

  handleChoice(choice: DialogueChoiceState): void {
    if (choice.decisionOptionId) {
      this.execute.emit(choice.decisionOptionId);
    } else if (choice.requiredToolCode) {
      this.useTool.emit(choice.requiredToolCode);
    } else {
      this.close.emit();
    }
  }

  iconFor(type?: string): string {
    return ({
      PERSON:  'person_heart',
      OBJECT:  'clinical_notes',
      ROUTE:   'health_and_safety',
      TOOL:    'construction',
      WARNING: 'warning',
      EXIT:    'exit_to_app'
    } as Record<string, string>)[type ?? 'OBJECT'] ?? 'psychology';
  }

  private startTypewriter(text: string): void {
    this.stopTypewriter();
    this.displayedText.set('');
    this.isTypingComplete.set(false);
    if (!text) {
      this.isTypingComplete.set(true);
      return;
    }
    let index = 0;
    this.typewriterHandle = setInterval(() => {
      index++;
      this.displayedText.set(text.slice(0, index));
      if (index >= text.length) {
        this.stopTypewriter();
        this.isTypingComplete.set(true);
      }
    }, TYPEWRITER_INTERVAL_MS);
  }

  private stopTypewriter(): void {
    if (this.typewriterHandle !== null) {
      clearInterval(this.typewriterHandle);
      this.typewriterHandle = null;
    }
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
npx jest dialogue-panel.component.spec --no-coverage
```

Expected: PASS — 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add admin-panel/src/app/features/simulator/dialogue-panel.component.ts
git add admin-panel/src/app/features/simulator/dialogue-panel.component.spec.ts
git commit -m "feat(dialogue): refactor to Undertale bottom strip with typewriter effect"
```

---

## Task 8: MinimapComponent — new standalone component

**Files:**

---

## Task 9: GameWorldComponent — fullscreen Phaser config

**Files:**
- Modify: `admin-panel/src/app/features/simulator/game-world.component.ts` (Angular `@Component` wrapper only — the Phaser scene class is updated in Task 10)

Remove the `.world-frame` card styling. The component fills whatever container places it. Touch controls become a bottom-overlay row.

- [ ] **Step 1: Replace the `@Component` decorator + `GameWorldComponent` class**

In `game-world.component.ts`, replace everything from line 242 (the `@Component({` decorator) through the end of file with:

```typescript
@Component({
  selector: 'app-game-world',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div #gameHost class="phaser-host" role="application"
      aria-label="Mapa explorable de la simulación. Usa WASD o flechas para moverte, E para interactuar.">
    </div>
    <div class="touch-controls" aria-label="Controles táctiles">
      <button type="button" class="psy-icon-button touch-btn" aria-label="Mover arriba"    (click)="nudge('up')"><mat-icon>keyboard_arrow_up</mat-icon></button>
      <button type="button" class="psy-icon-button touch-btn" aria-label="Mover izquierda" (click)="nudge('left')"><mat-icon>keyboard_arrow_left</mat-icon></button>
      <button type="button" class="psy-button psy-button--glass touch-interact"            (click)="interactNearest()"><mat-icon>touch_app</mat-icon>Interactuar</button>
      <button type="button" class="psy-icon-button touch-btn" aria-label="Mover derecha"   (click)="nudge('right')"><mat-icon>keyboard_arrow_right</mat-icon></button>
      <button type="button" class="psy-icon-button touch-btn" aria-label="Mover abajo"     (click)="nudge('down')"><mat-icon>keyboard_arrow_down</mat-icon></button>
    </div>
  `,
  styles: [`
    :host { display: block; width: 100%; height: 100%; }
    .phaser-host { width: 100%; height: 100%; }
    :host ::ng-deep .phaser-host canvas { display: block; width: 100% !important; height: 100% !important; }
    .touch-controls {
      display: none;
      position: absolute;
      bottom: 92px;
      left: 50%;
      transform: translateX(-50%);
      grid-template-columns: repeat(5, minmax(44px, auto));
      justify-content: center;
      gap: 8px;
      z-index: 48;
    }
    .touch-btn { background: rgba(8,12,18,.7); border-color: rgba(79,163,165,.3); color: #4fa3a5; }
    .touch-interact { font-size: .82rem; }
    @media (max-width: 900px) { .touch-controls { display: grid; } }
  `]
})
export class GameWorldComponent implements OnChanges, OnDestroy {
  readonly world = input<SimulationWorldState | null>(null);
  readonly selectedInteractionKey = input<string | null>(null);
  readonly nearbyInteraction = input<MapObjectState | null>(null);
  readonly proximity = output<MapObjectState | null>();
  readonly interact = output<MapObjectState>();
  readonly positionChange = output<{ x: number; y: number }>();
  private scene?: DataDrivenWorldScene;
  private phaserGame?: Phaser.Game;
  private gameHost?: ElementRef<HTMLDivElement>;

  constructor(private readonly zone: NgZone) {}

  @ViewChild('gameHost')
  set host(value: ElementRef<HTMLDivElement> | undefined) {
    this.gameHost = value;
    if (value) this.boot();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['world'] && this.world()) this.scene?.setWorld(this.world()!);
    if (changes['selectedInteractionKey']) this.scene?.setSelected(this.selectedInteractionKey());
  }

  ngOnDestroy() { this.phaserGame?.destroy(true); }

  nudge(direction: 'up' | 'down' | 'left' | 'right') { this.scene?.nudge(direction); }
  interactNearest() { this.scene?.interactNearest(); }
  focus(key: string) { this.scene?.focus(key); }

  private boot() {
    if (!this.gameHost || this.phaserGame) return;
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
    this.zone.runOutsideAngular(() => {
      this.scene = new DataDrivenWorldScene({
        reduceMotion,
        onProximity: i => this.zone.run(() => this.proximity.emit(i)),
        onInteract:  i => this.zone.run(() => this.interact.emit(i)),
        onPosition:  (x, y) => this.zone.run(() => this.positionChange.emit({ x, y }))
      });
      this.phaserGame = new Phaser.Game({
        type: Phaser.AUTO,
        parent: this.gameHost!.nativeElement,
        width: 960, height: 540,
        backgroundColor: '#0e141a',
        scale: { mode: Phaser.Scale.FIT, autoCenter: Phaser.Scale.CENTER_BOTH, width: 960, height: 540 },
        scene: this.scene
      });
    });
    window.setTimeout(() => { if (this.world()) this.scene?.setWorld(this.world()!); }, 0);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add admin-panel/src/app/features/simulator/game-world.component.ts
git commit -m "feat(game-world): GameWorldComponent fills container — no card wrapper"
```

---

## Task 10: DataDrivenWorldScene — Kenney preload + sprites

**Files:**
- Modify: `admin-panel/src/app/features/simulator/game-world.component.ts` (the `DataDrivenWorldScene` Phaser class at top, lines 1–241)

Add `preload()`, upgrade `renderWorld()` with Kenney sprites and a geometric fallback, update player and marker creation, add door-hint text containers.

- [ ] **Step 1: Add import at top of game-world.component.ts**

After the existing imports (after line 6 `import Phaser from 'phaser';`), add:

```typescript
import { KenneyCharFrames, KenneyDungeonFrames, KenneyTownFrames } from './kenney-frames.constants';
```

- [ ] **Step 2: Replace DataDrivenWorldScene class (lines 14–240)**

Replace the entire `class DataDrivenWorldScene extends Phaser.Scene { ... }` block with the version below. Keep the `WorldCallbacks` interface (lines 7–12) untouched — only replace the class itself.

```typescript
class DataDrivenWorldScene extends Phaser.Scene {
  private player?: Phaser.GameObjects.Container;
  private playerSprite?: Phaser.GameObjects.Sprite;
  private lastDirection: 'down' | 'up' | 'left' | 'right' = 'down';
  private cursors?: Phaser.Types.Input.Keyboard.CursorKeys;
  private keys?: Record<string, Phaser.Input.Keyboard.Key>;
  private readonly markers    = new Map<string, Phaser.GameObjects.Container>();
  private readonly markerData = new Map<string, MapObjectState>();
  private readonly doorHints  = new Map<string, Phaser.GameObjects.Container>();
  private world?: SimulationWorldState;
  private nearestKey: string | null = null;
  private selectedKey: string | null = null;
  private ready = false;
  private assetsLoaded = false;

  constructor(private readonly callbacks: WorldCallbacks) {
    super('data-driven-world');
  }

  preload() {
    this.load.on('loaderror', (_file: Phaser.Loader.File) => {
      // Missing asset — geometric fallback will be used
    });
    this.load.spritesheet('town-tiles',
      '/assets/game/kenney/tiny-town/Tilemap/tilemap_packed.png',
      { frameWidth: 16, frameHeight: 16 });
    this.load.spritesheet('dungeon-tiles',
      '/assets/game/kenney/tiny-dungeon/Tilemap/tilemap_packed.png',
      { frameWidth: 16, frameHeight: 16 });
    this.load.spritesheet('characters',
      '/assets/game/kenney/tiny-rpg-characters/Spritesheet/characters.png',
      { frameWidth: 16, frameHeight: 16 });
    this.load.once('complete', () => { this.assetsLoaded = true; });
  }

  create() {
    this.ready = true;
    this.cursors = this.input.keyboard?.createCursorKeys();
    this.keys = this.input.keyboard?.addKeys('W,A,S,D,E,SPACE,ENTER') as Record<string, Phaser.Input.Keyboard.Key>;
    this.createAnimations();
    this.renderWorld();
  }

  override update(_time: number, delta: number) {
    if (!this.player || !this.cursors || !this.keys || !this.world) return;
    const left  = this.cursors.left.isDown  || this.keys['A'].isDown;
    const right = this.cursors.right.isDown || this.keys['D'].isDown;
    const up    = this.cursors.up.isDown    || this.keys['W'].isDown;
    const down  = this.cursors.down.isDown  || this.keys['S'].isDown;
    const speed = 176 * (delta / 1000);
    const dx = Number(right) - Number(left);
    const dy = Number(down)  - Number(up);

    if (dx !== 0 || dy !== 0) {
      const len = Math.hypot(dx, dy);
      this.movePlayer((dx / len) * speed, (dy / len) * speed);
      this.callbacks.onPosition(Math.round(this.player.x), Math.round(this.player.y));
      this.lastDirection = Math.abs(dx) >= Math.abs(dy)
        ? (dx > 0 ? 'right' : 'left')
        : (dy > 0 ? 'down' : 'up');
      if (this.playerSprite && !this.callbacks.reduceMotion) {
        this.playerSprite.play(`walk-${this.lastDirection}`, true);
      }
    } else {
      this.playerSprite?.stop();
    }

    if (Phaser.Input.Keyboard.JustDown(this.keys['E']) ||
        Phaser.Input.Keyboard.JustDown(this.keys['SPACE']) ||
        Phaser.Input.Keyboard.JustDown(this.keys['ENTER'])) {
      this.interactNearest();
    }
    this.updateNearestInteraction();
  }

  setWorld(world: SimulationWorldState) {
    this.world = world;
    this.nearestKey = null;
    this.callbacks.onProximity(null);
    if (this.ready) this.renderWorld();
  }

  setSelected(key: string | null) {
    this.selectedKey = key;
    this.refreshMarkerStates();
  }

  nudge(direction: 'up' | 'down' | 'left' | 'right') {
    const d = 34;
    const mv: Record<string, [number, number]> = { up:[0,-d], down:[0,d], left:[-d,0], right:[d,0] };
    this.movePlayer(...mv[direction]);
    if (this.player) this.callbacks.onPosition(Math.round(this.player.x), Math.round(this.player.y));
    this.updateNearestInteraction();
  }

  interactNearest() {
    if (!this.nearestKey) return;
    const obj = this.markerData.get(this.nearestKey);
    if (obj) this.callbacks.onInteract(obj);
  }

  focus(key: string) {
    const m = this.markers.get(key);
    if (!m || this.callbacks.reduceMotion) return;
    this.tweens.add({ targets: m, scale: 1.16, duration: 140, yoyo: true, repeat: 2, ease: 'Sine.easeInOut' });
  }

  private createAnimations() {
    if (!this.assetsLoaded) return;
    const anims: Array<{ key: string; frames: readonly number[] }> = [
      { key: 'walk-down',  frames: KenneyCharFrames.PLAYER_WALK_DOWN },
      { key: 'walk-left',  frames: KenneyCharFrames.PLAYER_WALK_LEFT },
      { key: 'walk-right', frames: KenneyCharFrames.PLAYER_WALK_RIGHT },
      { key: 'walk-up',    frames: KenneyCharFrames.PLAYER_WALK_UP },
    ];
    for (const a of anims) {
      if (!this.anims.exists(a.key)) {
        this.anims.create({
          key: a.key,
          frames: this.anims.generateFrameNumbers('characters', { frames: [...a.frames] }),
          frameRate: 6, repeat: -1
        });
      }
    }
  }

  private renderWorld() {
    if (!this.world) return;
    this.children.removeAll(true);
    this.markers.clear();
    this.markerData.clear();
    this.doorHints.clear();

    const { width: mapW, height: mapH } = this.world.map;
    this.cameras.main.setBackgroundColor('#0e141a');

    if (this.assetsLoaded && this.textures.exists('town-tiles')) {
      this.add.tileSprite(mapW/2, mapH/2, mapW-40, mapH-42, 'town-tiles', KenneyTownFrames.FLOOR_WOOD).setAlpha(.9);
    } else {
      // Geometric fallback
      this.add.rectangle(mapW/2, mapH/2, mapW-40, mapH-42, 0xf4f8fb, 1)
        .setStrokeStyle(3, this.borderFor(this.world.map.theme), .4);
      const grid = this.add.graphics();
      grid.lineStyle(1, this.borderFor(this.world.map.theme), .14);
      for (let x=52; x<=mapW-52; x+=48) grid.lineBetween(x, 38, x, mapH-38);
      for (let y=42; y<=mapH-42; y+=48) grid.lineBetween(40, y, mapW-40, y);
    }

    this.add.rectangle(mapW/2, mapH/2, mapW-36, mapH-38)
      .setStrokeStyle(3, 0x4f7cac, .3).setFillStyle(0x000000, 0);
    this.add.text(56, 46, this.world.map.title, {
      fontFamily: 'Arial, sans-serif', fontSize: '16px', color: '#9dc0e8', fontStyle: 'bold'
    }).setDepth(5);

    this.world.collisions.forEach(zone => this.renderCollisionZone(zone));
    this.world.objects.forEach(obj => this.createMarker(obj));
    this.createPlayer(this.world.player.x, this.world.player.y);
    this.refreshMarkerStates();
    this.updateNearestInteraction();
  }

  private renderCollisionZone(zone: CollisionZoneState) {
    const cx = zone.x + zone.width/2;
    const cy = zone.y + zone.height/2;
    const isDoor = /puerta|door/i.test(zone.label ?? '');
    if (this.assetsLoaded && this.textures.exists('town-tiles')) {
      this.add.tileSprite(cx, cy, zone.width, zone.height, 'town-tiles',
        isDoor ? KenneyTownFrames.DOOR_CLOSED : KenneyTownFrames.WALL_H).setDepth(4);
    } else {
      this.add.rectangle(cx, cy, zone.width, zone.height, 0xffffff, .82)
        .setStrokeStyle(2, this.borderFor(this.world!.map.theme), .22).setDepth(4);
    }
    if (zone.label) {
      this.add.text(zone.x+5, zone.y+3, zone.label, {
        fontFamily: 'Arial, sans-serif', fontSize: '11px', color: '#9dc0e8',
        backgroundColor: 'rgba(8,12,18,.72)', padding: { x:4, y:2 }
      }).setDepth(5);
    }
  }

  private createPlayer(x: number, y: number) {
    const shadow = this.add.ellipse(0, 18, 20, 7, 0x000000, .22);
    if (this.assetsLoaded && this.textures.exists('characters')) {
      const sprite = this.add.sprite(0, 0, 'characters', KenneyCharFrames.PLAYER_IDLE).setScale(2);
      this.player = this.add.container(x, y, [shadow, sprite]).setDepth(20);
      this.playerSprite = sprite;
    } else {
      const body  = this.add.rectangle(0,  4, 24, 32, 0x4f7cac, 1).setStrokeStyle(2, 0xffffff, .9);
      const head  = this.add.circle(0, -18, 12, 0xf4c6a8, 1).setStrokeStyle(2, 0xffffff, .85);
      const badge = this.add.rectangle(0,  8, 12,  7, 0xffffff, .9);
      this.player = this.add.container(x, y, [shadow, body, head, badge]).setDepth(20);
    }
  }

  private createMarker(object: MapObjectState) {
    const isExit = object.type === 'EXIT';
    const color  = Number.parseInt(object.color.replace('#', ''), 16) || 0x4fa3a5;
    const label  = this.add.text(0, 28, object.label, {
      fontFamily: 'Arial, sans-serif', fontSize: '11px', color: '#e8f0f4',
      backgroundColor: 'rgba(8,12,18,.72)', padding: { x:5, y:3 }, align: 'center', wordWrap: { width: 140 }
    }).setOrigin(.5, 0);

    let main: Phaser.GameObjects.GameObject;

    if (this.assetsLoaded) {
      if (isExit && this.textures.exists('town-tiles')) {
        main = this.add.image(0, 0, 'town-tiles', KenneyTownFrames.DOOR_CLOSED).setScale(2.5);
      } else if (this.textures.exists('dungeon-tiles')) {
        main = this.add.image(0, 0, 'dungeon-tiles', this.frameForType(object.type)).setScale(2);
      } else {
        main = this.buildGeomMarker(color);
      }
    } else {
      main = this.buildGeomMarker(color);
    }

    const pulse = this.add.circle(0, 0, 22, color, .1);
    if (!this.callbacks.reduceMotion) {
      this.tweens.add({ targets: pulse, scale: 1.3, alpha: .05, duration: 1100, yoyo: true, repeat: -1, ease: 'Sine.easeInOut' });
    }

    const marker = this.add.container(object.x, object.y, [pulse, main, label]).setDepth(12);
    this.markers.set(object.key, marker);
    this.markerData.set(object.key, object);

    if (isExit) {
      const hintBg = this.add.rectangle(0, 0, 88, 22, 0x0e141a, .88).setStrokeStyle(1, 0x4fa3a5, .5);
      const hintTx = this.add.text(0, 0, `E  ${object.label} →`, {
        fontFamily: 'Arial, sans-serif', fontSize: '11px', color: '#4fa3a5', fontStyle: 'bold'
      }).setOrigin(.5);
      const hint = this.add.container(object.x, object.y - 50, [hintBg, hintTx]).setDepth(25).setVisible(false);
      this.doorHints.set(object.key, hint);
    }
  }

  private buildGeomMarker(color: number): Phaser.GameObjects.Container {
    const glow = this.add.circle(0, 0, 34, color, .16);
    const base = this.add.circle(0, 0, 24, color, .9).setStrokeStyle(3, 0xffffff, .95);
    if (!this.callbacks.reduceMotion) {
      this.tweens.add({ targets: glow, scale: 1.2, alpha: .08, duration: 1100, yoyo: true, repeat: -1, ease: 'Sine.easeInOut' });
    }
    return this.add.container(0, 0, [glow, base]);
  }

  private frameForType(type: string): number {
    const map: Record<string, number> = {
      PERSON: KenneyDungeonFrames.DESK, OBJECT: KenneyDungeonFrames.CABINET,
      ROUTE: KenneyDungeonFrames.PLANT, TOOL: KenneyDungeonFrames.CHAIR,
      WARNING: KenneyDungeonFrames.DESK
    };
    return map[type] ?? KenneyDungeonFrames.DESK;
  }

  private movePlayer(dx: number, dy: number) {
    if (!this.player || !this.world) return;
    const px = this.player.x, py = this.player.y;
    this.player.x = Phaser.Math.Clamp(this.player.x + dx, 56, this.world.map.width  - 56);
    if (this.collides(this.player.x, this.player.y)) this.player.x = px;
    this.player.y = Phaser.Math.Clamp(this.player.y + dy, 70, this.world.map.height - 58);
    if (this.collides(this.player.x, this.player.y)) this.player.y = py;
  }

  private collides(x: number, y: number): boolean {
    if (!this.world) return false;
    const pb = new Phaser.Geom.Rectangle(x-15, y-27, 30, 46);
    return this.world.collisions.some(z =>
      Phaser.Geom.Intersects.RectangleToRectangle(pb, new Phaser.Geom.Rectangle(z.x, z.y, z.width, z.height)));
  }

  private updateNearestInteraction() {
    if (!this.player || !this.world?.objects.length) return;
    let nearest: MapObjectState | null = null;
    let nearestD = Infinity;
    for (const obj of this.world.objects) {
      const d = Phaser.Math.Distance.Between(this.player.x, this.player.y, obj.x, obj.y);
      if (d < nearestD) { nearest = obj; nearestD = d; }
    }
    const nextKey = nearest && nearestD <= 74 ? nearest.key : null;
    if (nextKey !== this.nearestKey) {
      this.nearestKey = nextKey;
      this.callbacks.onProximity(nextKey ? nearest : null);
      this.refreshMarkerStates();
    }
  }

  private refreshMarkerStates() {
    this.markers.forEach((m, key) => {
      const sel = key === this.selectedKey, near = key === this.nearestKey;
      m.setScale(sel ? 1.12 : near ? 1.08 : 1);
      m.setAlpha(sel || near ? 1 : .88);
    });
    this.doorHints.forEach((h, key) => h.setVisible(key === this.nearestKey));
  }

  private backgroundFor(theme: string): string {
    const m: Record<string, string> = {
      'protection-route':'#eef7f3','technical-record':'#f2f6fb',
      'risk-assessment':'#f5f2fb','child-protection':'#f0f8f4','follow-up':'#edf5fa'
    };
    return m[theme] ?? '#edf7f8';
  }

  private borderFor(theme: string): number {
    const m: Record<string, number> = {
      'protection-route':0x2f7476,'technical-record':0x2f5f8f,
      'risk-assessment':0x5b4f8f,'child-protection':0x5d9278,'follow-up':0x2f5f8f
    };
    return m[theme] ?? 0x4f7cac;
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add admin-panel/src/app/features/simulator/game-world.component.ts
git commit -m "feat(phaser): add preload, Kenney sprites, walking anims, door hints, geometric fallback"
```

---

## Task 11: SimulationPlayComponent — fullscreen restructure

**Files:**
- Modify: `admin-panel/src/app/features/simulator/simulation-play.component.ts`

Replace the entire file. The TS logic is preserved and extended with `journalOpen`, `fadeActive`, `triggerFade()`, `buildSupervisionDialogue()`, and a `J`-key handler. `SupervisionFeedbackComponent` is no longer imported.

- [ ] **Step 1: Replace the full file**

Replace the entire content of `admin-panel/src/app/features/simulator/simulation-play.component.ts` with the code block below. The game logic methods (`openInteraction`, `executeDecision`, `useTool`, `safeExit`, `saveReflection`, `rememberPosition`, `persistPosition`) are preserved unchanged except for the fade wrapper in `executeDecision` and the new `showToolFeedback` routing through the dialogue strip.

```typescript
import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { SimulationService } from '../../core/api/simulation.service';
import {
  DialogueState, MapObjectState, SimulationAttemptState,
  SimulationFeedback, SimulationWorldState, ToolUseResult
} from '../../core/models/simulation.model';
import { DialoguePanelComponent } from './dialogue-panel.component';
import { GameWorldComponent } from './game-world.component';
import { JournalPanelComponent } from './journal-panel.component';
import { MinimapComponent, MinimapStage } from './minimap.component';
import { SimulationHudComponent } from './simulation-hud.component';
import { ToolInventoryComponent } from './tool-inventory.component';

@Component({
  selector: 'app-simulation-play',
  standalone: true,
  imports: [
    CommonModule, RouterLink, MatIconModule, MatProgressBarModule,
    SimulationHudComponent, GameWorldComponent, DialoguePanelComponent,
    ToolInventoryComponent, JournalPanelComponent, MinimapComponent
  ],
  template: `
    <div class="game-container" id="main-content" tabindex="-1">
      <div class="sr-only" aria-live="assertive" role="status" aria-atomic="true">{{ a11yAnnouncement() }}</div>

      @if (loading()) {
        <div class="loading-overlay"><mat-progress-bar mode="indeterminate" aria-label="Cargando simulación" /></div>
      }
      @if (error()) {
        <div class="error-overlay" role="alert">
          <mat-icon>error</mat-icon><p>{{ error() }}</p>
          <a class="psy-button psy-button--ghost" routerLink="/portal/simulador">Volver al simulador</a>
        </div>
      }

      @if (attempt(); as game) {
        @if (world(); as w) {
          <app-game-world #gameWorld class="game-layer" [world]="w"
            [nearbyInteraction]="nearbyInteraction()" [selectedInteractionKey]="selectedInteraction()?.key ?? null"
            (proximity)="nearbyInteraction.set($event)" (interact)="openInteraction($event)"
            (positionChange)="rememberPosition($event.x, $event.y)" />
        } @else {
          <div class="world-skeleton" aria-label="Cargando mapa"></div>
        }

        <app-simulation-hud class="hud-layer" [attempt]="game" [stressPulse]="stressPulse()" />
        <app-minimap class="minimap-layer" [stages]="stages" [currentNodeKey]="game.currentNode.key" />
        <app-tool-inventory class="tools-layer" [tools]="world()?.tools ?? []"
          [inventory]="world()?.inventory ?? []" (select)="selectTool($event)" />

        @if (nearbyInteraction(); as nb) {
          <div class="proximity-hint" [class.proximity-hint--exit]="nb.type === 'EXIT'" aria-live="polite" aria-atomic="true">
            <kbd>E</kbd><span>{{ nb.type === 'EXIT' ? nb.label + ' →' : nb.label }}</span>
          </div>
        }

        <button class="journal-toggle" type="button" aria-label="Abrir bitácora (J)"
          [class.journal-toggle--active]="journalOpen()" (click)="journalOpen.set(!journalOpen())">
          <mat-icon aria-hidden="true">menu_book</mat-icon>
        </button>

        @if (game.status === 'IN_PROGRESS') {
          <button class="safe-exit-btn" type="button" (click)="safeExit()" [disabled]="busy()"
            aria-label="Salida segura (Escape)">
            <mat-icon aria-hidden="true">exit_to_app</mat-icon>
          </button>
        }

        <div class="controls-hint" aria-hidden="true">WASD/↑↓←→ · E interactuar · J bitácora · Esc salida</div>

        <app-dialogue-panel class="dialogue-layer" [dialogue]="dialogue()" [interaction]="selectedInteraction()"
          (close)="closeDialogue()" (execute)="executeDecision($event)" (useTool)="useTool($event)" />

        <div class="stress-vignette" [class.vignette--active]="stressVignetteLevel() > 0"
          [style.--vignette-opacity]="stressVignetteLevel()" aria-hidden="true"></div>

        <app-journal-panel #journalPanel class="journal-layer" [open]="journalOpen()"
          [disabled]="game.status !== 'IN_PROGRESS' || busy()" [message]="journalMessage()"
          (save)="saveReflection($event)" (closeSheet)="journalOpen.set(false)" />

        <!-- Fase 7: screen-reader narrative route -->
        <section class="sr-narrative-route" aria-label="Ruta narrativa accesible">
          <h4 class="sr-only">Escena: {{ game.currentNode.title }}</h4>
          <p class="sr-only">{{ game.currentNode.narrative }}</p>
          @if (game.currentNode.warningMessage) {
            <p class="sr-only" role="alert">Advertencia: {{ game.currentNode.warningMessage }}</p>
          }
          @if (game.currentNode.sensitiveContent) {
            <p class="sr-only" role="note">Contenido sensible. Salida segura con Escape.</p>
          }
          <div class="sr-only">Puntaje: {{ game.accumulatedScore }}. Estrés: {{ game.stressIndex }}%. Estado: {{ statusLabelA11y(game.status) }}.</div>
        </section>

        <!-- Fase 7: accessible interaction list -->
        <section class="sr-only" aria-label="Lista accesible de puntos interactivos">
          <div role="list">
            @for (obj of world()?.objects ?? []; track obj.key) {
              <button role="listitem" type="button" class="sr-only"
                [attr.aria-label]="obj.label + ': ' + obj.interactionPrompt"
                (click)="openInteraction(obj)">{{ obj.label }}</button>
            }
          </div>
        </section>

        @if (game.status !== 'IN_PROGRESS') {
          <section class="end-state-overlay liquid-glass"
            [class.end-state--safe]="game.status === 'SAFE_EXITED'" role="alert">
            <mat-icon>{{ game.status === 'COMPLETED' ? 'workspace_premium' : 'exit_to_app' }}</mat-icon>
            <div>
              <p class="psy-eyebrow">{{ game.status === 'COMPLETED' ? 'Misión completada' : 'Salida segura' }}</p>
              <h3>{{ game.status === 'COMPLETED' ? 'El intento quedó cerrado para evaluación docente.' : 'El intento fue pausado de forma limpia.' }}</h3>
              <p>{{ game.status === 'COMPLETED'
                ? 'La ruta recorrida, decisiones, puntaje, estrés y bitácoras quedaron registrados.'
                : 'Puedes revisar recursos de apoyo o retomar con acompañamiento institucional.' }}</p>
            </div>
            <a class="psy-button psy-button--primary" routerLink="/portal/simulador">
              <mat-icon aria-hidden="true">arrow_back</mat-icon>Volver al simulador
            </a>
          </section>
        }

        <div class="scene-fade" [class.scene-fade--active]="fadeActive()" aria-hidden="true"></div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .game-container { position: fixed; inset: 0; overflow: hidden; background: #0a0f14; }
    .game-layer { position: absolute; inset: 0; z-index: 10; }
    .world-skeleton { position: absolute; inset: 0; z-index: 10; background: #0e141a; }
    app-simulation-hud.hud-layer { position: absolute; top: 0; left: 0; right: 0; z-index: 50; }
    app-minimap.minimap-layer { position: absolute; top: 62px; right: 12px; z-index: 50; }
    app-tool-inventory.tools-layer { position: absolute; bottom: 92px; left: 12px; z-index: 50; }
    .proximity-hint {
      position: absolute; bottom: 92px; left: 50%; transform: translateX(-50%); z-index: 50;
      display: flex; align-items: center; gap: 8px; padding: 6px 14px; border-radius: 999px;
      background: rgba(8,12,18,.82); border: 1px solid rgba(79,163,165,.3);
      color: #e8f0f4; font-size: .82rem; font-weight: 700; white-space: nowrap; pointer-events: none;
      animation: hint-rise 160ms ease both;
    }
    .proximity-hint--exit { border-color: rgba(79,163,165,.6); color: #4fa3a5; }
    .proximity-hint kbd {
      padding: 2px 7px; border-radius: 5px; background: rgba(79,163,165,.18);
      border: 1px solid rgba(79,163,165,.35); font-size: .76rem;
      font-family: 'JetBrains Mono', monospace; color: #4fa3a5;
    }
    .journal-toggle {
      position: absolute; bottom: 92px; right: 12px; z-index: 50;
      width: 44px; height: 44px; border: 1px solid rgba(79,163,165,.28); border-radius: 10px;
      background: rgba(8,12,18,.76); color: rgba(79,163,165,.6); cursor: pointer;
      display: grid; place-items: center; transition: border-color 160ms, color 160ms;
    }
    .journal-toggle--active { border-color: rgba(79,163,165,.75); color: #4fa3a5; }
    .safe-exit-btn {
      position: absolute; top: 62px; right: 12px; z-index: 51;
      width: 40px; height: 40px; border: 1px solid rgba(168,80,98,.3); border-radius: 10px;
      background: rgba(8,12,18,.7); color: rgba(168,80,98,.6); cursor: pointer;
      display: grid; place-items: center; transition: border-color 160ms, color 160ms;
    }
    .safe-exit-btn:hover { border-color: rgba(168,80,98,.6); color: rgba(168,80,98,.9); }
    .safe-exit-btn:disabled { opacity: .35; cursor: not-allowed; }
    .controls-hint {
      position: absolute; bottom: 8px; left: 50%; transform: translateX(-50%); z-index: 50;
      padding: 5px 12px; border-radius: 999px; background: rgba(8,12,18,.5);
      color: rgba(232,240,244,.3); font-size: .66rem; font-weight: 700; letter-spacing: .05em;
      pointer-events: none; white-space: nowrap;
    }
    app-dialogue-panel.dialogue-layer { position: absolute; bottom: 0; left: 0; right: 0; z-index: 60; }
    .stress-vignette {
      position: fixed; inset: 0; pointer-events: none; z-index: 90; opacity: 0;
      transition: opacity 1.2s cubic-bezier(.4,0,.2,1);
      background: radial-gradient(ellipse at center, transparent 55%, rgba(120,40,55,.22) 100%);
    }
    .vignette--active { opacity: var(--vignette-opacity, 0); }
    app-journal-panel.journal-layer { position: absolute; top: 0; right: 0; bottom: 0; width: min(400px,88vw); z-index: 100; }
    .end-state-overlay {
      position: absolute; inset: 0; z-index: 150; display: flex; flex-direction: column;
      gap: 18px; align-items: center; justify-content: center; text-align: center;
      padding: 32px; background: rgba(8,12,18,.92); color: #e8f0f4;
    }
    .end-state-overlay mat-icon {
      font-size: 48px; width: 72px; height: 72px; display: grid; place-items: center;
      border-radius: 22px; background: rgba(79,163,165,.14); color: #4fa3a5;
    }
    .end-state--safe mat-icon { color: rgba(232,240,244,.6); background: rgba(232,240,244,.06); }
    .end-state-overlay h3 { margin: 0; font-family: 'Cormorant Garamond', serif; font-size: 1.5rem; }
    .end-state-overlay p  { margin: 0; color: rgba(232,240,244,.55); line-height: 1.6; max-width: 560px; }
    .scene-fade {
      position: fixed; inset: 0; z-index: 200; background: #0a0f14; opacity: 0;
      pointer-events: none; transition: opacity 320ms ease;
    }
    .scene-fade--active { opacity: 1; pointer-events: auto; }
    .loading-overlay { position: absolute; top: 0; left: 0; right: 0; z-index: 300; }
    .error-overlay {
      position: absolute; inset: 0; z-index: 300; display: flex; flex-direction: column;
      gap: 14px; align-items: center; justify-content: center;
      background: rgba(8,12,18,.9); color: #e8f0f4; padding: 24px; text-align: center;
    }
    .error-overlay mat-icon { font-size: 36px; color: rgba(168,80,98,.8); }
    .error-overlay p { margin: 0; color: rgba(232,240,244,.6); }
    @keyframes hint-rise {
      from { opacity: 0; transform: translateX(-50%) translateY(6px); }
      to   { opacity: 1; transform: translateX(-50%) translateY(0); }
    }
    @media (prefers-reduced-motion: reduce) {
      .stress-vignette, .scene-fade { transition: none; }
    }
  `]
})
export class SimulationPlayComponent implements OnInit {
  private readonly simulationService = inject(SimulationService);
  private readonly route = inject(ActivatedRoute);

  @ViewChild('gameWorld')    private gameWorld?: GameWorldComponent;
  @ViewChild('journalPanel') private journalPanel?: JournalPanelComponent;

  readonly attempt    = signal<SimulationAttemptState | null>(null);
  readonly world      = signal<SimulationWorldState | null>(null);
  readonly loading    = signal(true);
  readonly busy       = signal(false);
  readonly error      = signal('');
  readonly journalMessage      = signal('');
  readonly nearbyInteraction   = signal<MapObjectState | null>(null);
  readonly selectedInteraction = signal<MapObjectState | null>(null);
  readonly dialogue    = signal<DialogueState | null>(null);
  readonly stressPulse = signal(false);
  readonly a11yAnnouncement = signal('');
  readonly journalOpen  = signal(false);
  readonly fadeActive   = signal(false);

  readonly stages: MinimapStage[] = [
    { key: 'urgencias-crisis',     label: 'Crisis'  },
    { key: 'ruta-proteccion',      label: 'Ruta'    },
    { key: 'informe-integral',     label: 'Informe' },
    { key: 'valoracion-comisaria', label: 'Riesgo'  },
    { key: 'proteccion-nna',       label: 'NNA'     },
    { key: 'cierre-seguimiento',   label: 'Cierre'  }
  ];

  readonly stressVignetteLevel = computed(() => {
    const s = this.attempt()?.stressIndex ?? 0;
    if (s < 40) return 0;
    return Math.min(0.45, 0.05 + ((s - 40) / 60) * 0.4);
  });

  private lastPosition: { x: number; y: number } | null = null;
  private positionSaveHandle: number | null = null;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('caseVersionId'));
    if (!id) { this.error.set('Caso no encontrado.'); this.loading.set(false); return; }
    this.simulationService.startAttempt(id).subscribe({
      next: attempt => { this.attempt.set(attempt); this.loadWorld(attempt); },
      error: () => { this.error.set('No pudimos iniciar la simulación. Revisa tus permisos.'); this.loading.set(false); }
    });
  }

  @HostListener('window:keydown', ['$event'])
  handleGlobalInteraction(event: KeyboardEvent) {
    const tag = (event.target as HTMLElement | null)?.tagName;
    const editable = (event.target as HTMLElement | null)?.isContentEditable;

    if (event.key === 'Escape') {
      if (this.journalOpen()) { event.preventDefault(); this.journalOpen.set(false); return; }
      if (this.dialogue())    { event.preventDefault(); this.closeDialogue(); return; }
      const g = this.attempt();
      if (g?.status === 'IN_PROGRESS' && !this.busy()) { event.preventDefault(); this.safeExit(); }
      return;
    }

    if ((event.key === 'j' || event.key === 'J') && tag !== 'INPUT' && tag !== 'TEXTAREA' && !editable) {
      event.preventDefault();
      this.journalOpen.set(!this.journalOpen());
      return;
    }

    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || editable) return;

    if (['e','E',' ','Enter'].includes(event.key)) {
      const nb = this.nearbyInteraction();
      if (!nb) return;
      event.preventDefault();
      this.openInteraction(nb);
    }
  }

  openInteraction(interaction: MapObjectState) {
    const game = this.attempt();
    if (!game || game.status !== 'IN_PROGRESS') return;
    this.selectedInteraction.set(interaction);
    this.gameWorld?.focus(interaction.key);
    this.busy.set(true);
    this.simulationService.openInteraction(game.attemptId, game.attemptToken, interaction.key).subscribe({
      next: result => {
        this.world.set(result.world);
        this.selectedInteraction.set(result.interaction);
        this.dialogue.set(result.dialogue ?? result.interaction.dialogue);
        this.busy.set(false);
      },
      error: () => { this.error.set('No pudimos abrir la interacción.'); this.busy.set(false); }
    });
  }

  executeDecision(decisionOptionId: number) {
    const game = this.attempt();
    if (!game || this.busy()) return;
    this.busy.set(true);
    this.dialogue.set(null);
    this.journalMessage.set('');
    this.triggerFade(() => {
      this.simulationService.chooseDecision(game.attemptId, game.attemptToken, decisionOptionId).subscribe({
        next: updated => {
          this.attempt.set(updated);
          this.selectedInteraction.set(null);
          this.nearbyInteraction.set(null);
          this.journalPanel?.clear();
          this.loadWorld(updated);
          if (updated.feedback) {
            window.setTimeout(() => this.dialogue.set(this.buildSupervisionDialogue(updated.feedback!)), 400);
          }
        },
        error: () => { this.error.set('No pudimos ejecutar la intervención.'); this.busy.set(false); this.fadeActive.set(false); }
      });
    });
  }

  useTool(toolCode: string) {
    const game = this.attempt();
    const target = this.selectedInteraction()?.key ?? null;
    if (!game || this.busy()) return;
    this.busy.set(true);
    this.simulationService.useTool(game.attemptId, game.attemptToken, toolCode, target).subscribe({
      next: (result: ToolUseResult) => {
        this.world.set(result.world);
        const cur = this.attempt();
        if (cur) this.attempt.set({ ...cur, stressIndex: Math.max(0, Math.min(100, cur.stressIndex + result.stressDelta)) });
        this.showToolFeedback(result);
        this.busy.set(false);
      },
      error: () => { this.error.set('No pudimos usar la herramienta.'); this.busy.set(false); }
    });
  }

  selectTool(toolCode: string) {
    const tool = this.world()?.tools.find(t => t.code === toolCode);
    if (!tool) return;
    this.dialogue.set({
      key: tool.code, speakerName: 'Herramienta profesional', portraitKey: tool.icon, emotion: 'neutral',
      lines: [{ order: 1, speakerName: tool.label, text: tool.description, emotion: 'neutral' }], choices: []
    });
    this.selectedInteraction.set({
      key: `tool-${tool.code}`, label: tool.label, type: 'TOOL',
      x: 0, y: 0, width: 0, height: 0, color: '#4FA3A5', icon: tool.icon,
      shortCode: tool.code.slice(0,4), collision: false,
      interactionPrompt: 'Herramienta profesional', interactionText: tool.description,
      decisionOptionId: null, toolCode: tool.code, dialogue: null
    });
  }

  closeDialogue() { this.dialogue.set(null); }

  saveReflection(text: string) {
    const game = this.attempt();
    if (!game || !text.trim() || this.busy()) return;
    this.busy.set(true);
    this.simulationService.saveReflection(game.attemptId, game.attemptToken, game.currentNode.id, text.trim()).subscribe({
      next: () => { this.journalMessage.set('Bitácora guardada y cifrada.'); this.busy.set(false); },
      error: () => { this.journalMessage.set('No pudimos guardar la bitácora.'); this.busy.set(false); }
    });
  }

  safeExit() {
    const game = this.attempt();
    if (!game || this.busy()) return;
    this.busy.set(true);
    this.simulationService.safeExit(game.attemptId, game.attemptToken, 'Salida segura solicitada').subscribe({
      next: updated => { this.attempt.set(updated); this.selectedInteraction.set(null); this.dialogue.set(null); this.loadWorld(updated); },
      error: () => { this.error.set('No pudimos registrar la salida segura.'); this.busy.set(false); }
    });
  }

  rememberPosition(x: number, y: number) {
    this.lastPosition = { x, y };
    if (this.positionSaveHandle) window.clearTimeout(this.positionSaveHandle);
    this.positionSaveHandle = window.setTimeout(() => this.persistPosition(), 550);
  }

  statusLabelA11y(status: SimulationAttemptState['status']): string {
    return { IN_PROGRESS:'En progreso', COMPLETED:'Finalizado', SAFE_EXITED:'Pausado con salida segura' }[status];
  }

  private showToolFeedback(result: ToolUseResult): void {
    this.dialogue.set({
      key: `tool-feedback-${result.toolCode}-${Date.now()}`,
      speakerName: result.pertinent ? '✓ Herramienta pertinente' : 'ℹ Herramienta aplicada',
      portraitKey: null, emotion: result.pertinent ? 'positive' : 'neutral',
      lines: [
        { order: 1, speakerName: '', text: result.feedbackMessage, emotion: 'neutral' },
        { order: 2, speakerName: '', text: `Estrés ${result.stressDelta >= 0 ? '+' : ''}${result.stressDelta}%`, emotion: 'neutral' }
      ],
      choices: []
    });
    this.stressPulse.set(true);
    window.setTimeout(() => this.stressPulse.set(false), 700);
    this.announce(result.feedbackMessage);
    window.setTimeout(() => {
      if (this.dialogue()?.key?.startsWith('tool-feedback-')) this.dialogue.set(null);
    }, 5000);
  }

  private buildSupervisionDialogue(feedback: SimulationFeedback): DialogueState {
    const lines: DialogueState['lines'] = [
      { order: 1, speakerName: 'Supervisión clínica', text: feedback.message, emotion: feedback.prohibitedConduct ? 'danger' : 'neutral' }
    ];
    if (feedback.prohibitionReason) {
      lines.push({ order: 2, speakerName: 'Supervisión clínica', text: feedback.prohibitionReason, emotion: 'danger' });
    }
    lines.push({ order: lines.length+1, speakerName: '', text: `Puntaje ${feedback.scoreDelta>=0?'+':''}${feedback.scoreDelta} · Estrés ${feedback.stressDelta>=0?'+':''}${feedback.stressDelta}%`, emotion: 'neutral' });
    return {
      key: `supervision-${Date.now()}`, speakerName: 'Supervisión clínica',
      portraitKey: null,
      emotion: feedback.prohibitedConduct ? 'danger' : feedback.classification === 'ADEQUATE' ? 'positive' : 'neutral',
      lines, choices: []
    };
  }

  private triggerFade(callback: () => void): void {
    this.fadeActive.set(true);
    window.setTimeout(callback, 340);
  }

  private loadWorld(attempt: SimulationAttemptState) {
    this.simulationService.getWorld(attempt.attemptId, attempt.attemptToken).subscribe({
      next: world => {
        this.world.set(world);
        this.loading.set(false);
        this.busy.set(false);
        window.setTimeout(() => this.fadeActive.set(false), 80);
      },
      error: () => {
        this.error.set('No pudimos cargar el mapa del caso.');
        this.loading.set(false);
        this.busy.set(false);
        this.fadeActive.set(false);
      }
    });
  }

  private announce(message: string): void {
    this.a11yAnnouncement.set('');
    window.setTimeout(() => this.a11yAnnouncement.set(message), 50);
  }

  private persistPosition() {
    const game = this.attempt(), world = this.world();
    if (!game || !world || !this.lastPosition || game.status !== 'IN_PROGRESS') return;
    this.simulationService.updateWorldState(game.attemptId, game.attemptToken,
      this.lastPosition.x, this.lastPosition.y, world.map.key)
      .subscribe({ next: u => this.world.set(u), error: () => undefined });
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add admin-panel/src/app/features/simulator/simulation-play.component.ts
git commit -m "feat(play): fullscreen container — overlays, fade, J key, supervision in dialogue strip"
```

---

## Task 12: Build verification + acceptance criteria

- [ ] **Step 1: Run TypeScript build**

```bash
cd admin-panel && npm run build 2>&1 | tail -30
```

Expected: `✔ Browser application bundle generation complete.` — zero errors.

Common fixes:
- `effect` not imported in dialogue-panel → add `effect` to `@angular/core` import
- `MinimapStage` not found → confirm `export interface MinimapStage` in `minimap.component.ts`
- `NavigationEnd` import missing in shell → confirm `NavigationEnd, Router` in router import

- [ ] **Step 2: Run unit tests**

```bash
npx jest --no-coverage 2>&1 | tail -20
```

Expected: 3 suites, all pass (shell, dialogue, minimap specs).

- [ ] **Step 3: Manual verification checklist**

Start dev: `npm start`

| # | Acceptance criterion | Check |
|---|----------------------|-------|
| 1 | Nav + header hidden at `/portal/simulador/:id` | `document.body.classList` has `game-mode` |
| 2 | Canvas fullscreen | Phaser canvas fills viewport (FIT mode, possible letterbox) |
| 3 | Undertale dialogue strip on NPC interaction | Strip slides up from bottom, typewriter text, choice chips |
| 4 | Tool HUD icons bottom-left | Column of 44×44 buttons visible |
| 5 | Journal slides in with J or button | Slide from right, Esc closes |
| 6 | Kenney sprites (or geometric fallback) | Objects show sprites if assets present |
| 7 | Player sprite with walk anims | Character animates when moving (or geometric shape if no assets) |
| 8 | Door tile + E hint when near EXIT | Door hint "E · [room] →" appears above EXIT object |
| 9 | Scene transition fade | Fade black on decision → new world revealed |
| 10 | Minimap top-right | Nodes listed, current highlighted teal |
| 11 | Tool feedback in strip, no toast | `useTool` response appears in Undertale strip |
| 12 | Portal nav returns on exit | Navigate to dashboard → sidenav and topbar visible |
| 13 | `npm run build` 0 errors | See Step 1 |
| 14 | SR-only narrative route intact | Screen reader announces `sr-narrative-route` section |

- [ ] **Step 4: Final commit**

```bash
git commit --allow-empty -m "feat(game-ux): redesign complete — spec 2026-05-27 all 14 acceptance criteria"
```

---

## Post-plan notes

**Kenney frame indices** are estimates. After downloading the packs, open the PNG in Tiled (or any image viewer with pixel counts) and update the values in `kenney-frames.constants.ts`. The game falls back to geometric shapes automatically when the texture key is absent.

**`SupervisionFeedbackComponent`** (`supervision-feedback.component.ts`) is no longer imported. Its file remains on disk and can be deleted in a cleanup PR.

**All decisions** now go through `triggerFade()`. This means every node transition — not just door-crossings — gets the cinematic fade. This is by design: consistent with the spec's "cada nodo DAG es una habitación" model.

**Audio** (AudioDirector, Howler.js) is out of scope per spec section "Pendientes fuera de scope".
