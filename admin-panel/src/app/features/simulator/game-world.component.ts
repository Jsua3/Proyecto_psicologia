import { CommonModule } from '@angular/common';
import { Component, ElementRef, NgZone, OnChanges, OnDestroy, SimpleChanges, ViewChild, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import Phaser from 'phaser';
import { CollisionZoneState, MapObjectState, SimulationWorldState } from '../../core/models/simulation.model';
import { KenneyCharFrames, KenneyDungeonFrames } from './kenney-frames.constants';

interface WorldCallbacks {
  onProximity: (obj: MapObjectState | null) => void;
  onInteract:  (obj: MapObjectState) => void;
  onPosition:  (x: number, y: number) => void;
  reduceMotion: boolean;
}

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
      // Missing asset — fallback rendering used
    });

    // ── Plain images for Tiled tilemap layer rendering (all 3 Kenney packs) ──
    // tiny-dungeon: 12×11 = 132 tiles  (GID  1–132)
    // tiny-town:    12×11 = 132 tiles  (GID  133–264)
    // rpg-urban:    27×18 = 486 tiles  (GID  265–750)
    this.load.image('dungeon-img', '/assets/game/kenney/tiny-dungeon/Tilemap/tilemap_packed.png');
    this.load.image('town-img',   '/assets/game/kenney/tiny-town/Tilemap/tilemap_packed.png');
    this.load.image('rpg-img',    '/assets/game/kenney/rpg-urban-pack/Spritesheet/tilemap_packed.png');

    // Dungeon spritesheet (used for interactive object marker icons)
    this.load.spritesheet('dungeon-tiles',
      '/assets/game/kenney/tiny-dungeon/Tilemap/tilemap_packed.png',
      { frameWidth: 16, frameHeight: 16 });

    // Character spritesheet (player + NPCs)
    this.load.spritesheet('characters',
      '/assets/game/kenney/rpg-urban-pack/Spritesheet/tilemap_packed.png',
      { frameWidth: 16, frameHeight: 16 });

    // Tiled JSON maps — all known scenario keys (missing ones fail silently)
    // Rename the Tiled object "name" field to match your backend object keys.
    const scenarioKeys = [
      'urgencias-crisis', 'ruta-proteccion', 'informe-integral',
      'valoracion-comisaria', 'proteccion-nna', 'cierre-seguimiento'
    ];
    for (const key of scenarioKeys) {
      this.load.tilemapTiledJSON(`map-${key}`, `/assets/game/maps/${key}.json`);
    }

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
      if (this.playerSprite) {
        if (this.lastDirection === 'left')  this.playerSprite.setFlipX(true);
        if (this.lastDirection === 'right') this.playerSprite.setFlipX(false);
        if (!this.callbacks.reduceMotion) this.playerSprite.play(`walk-${this.lastDirection}`, true);
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

    const mapKey  = this.world.map.key;
    const { width: mapW, height: mapH } = this.world.map;
    this.cameras.main.setBackgroundColor('#0e141a');

    // ── Layer 0-1: procedural dark floor + grid (always — permanent base) ──
    // Tiled floor tiles sit on top; GID 0 cells (empty) let this base show through.
    this.add.rectangle(mapW/2, mapH/2, mapW-40, mapH-42, 0x131c28, 1).setDepth(0);
    const g = this.add.graphics().setDepth(1);
    g.lineStyle(1, 0x1c2d3e, 0.65);
    for (let x = 56; x <= mapW-56; x += 32) g.lineBetween(x, 44, x, mapH-44);
    for (let y = 56; y <= mapH-56; y += 32) g.lineBetween(44, y, mapW-44, y);

    // ── Layer 2-3: Tiled tile layers (Floor + Walls) if map available ─────
    // Floor layer starts empty (all GID 0 = transparent).
    // Open any map in Tiled editor, paint tiles on the Floor layer, save → renders here.
    let tiledObjects: Phaser.Types.Tilemaps.TiledObject[] = [];
    let hasTiledMap = false;

    if (this.assetsLoaded) {
      try {
        const tilemap    = this.make.tilemap({ key: `map-${mapKey}` });
        // Register all 3 tilesets — only those present in the map JSON are used
        const ts1 = tilemap.addTilesetImage('tiny-dungeon', 'dungeon-img');
        const ts2 = tilemap.addTilesetImage('tiny-town',    'town-img');
        const ts3 = tilemap.addTilesetImage('rpg-urban',    'rpg-img');
        const tilesets = [ts1, ts2, ts3].filter((t): t is Phaser.Tilemaps.Tileset => t !== null);
        if (tilesets.length > 0) {
          tilemap.createLayer('Floor', tilesets)?.setDepth(2);
          tilemap.createLayer('Walls', tilesets)?.setDepth(3);
        }
        tiledObjects = tilemap.getObjectLayer('Objects')?.objects ?? [];
        hasTiledMap  = true;
      } catch {
        hasTiledMap = false;
      }
    }

    // Collision-zone panels only when no Tiled layout is available
    if (!hasTiledMap) {
      this.world.collisions.forEach(zone => this.renderCollisionZone(zone));
    }
    // ─────────────────────────────────────────────────────────────────────

    // Room border (depth 5) + title (depth 6) — always above tile layers (depth 2-3)
    this.add.rectangle(mapW/2, mapH/2, mapW-36, mapH-38)
      .setStrokeStyle(3, 0x4f7cac, .3).setFillStyle(0x000000, 0).setDepth(5);
    this.add.text(56, 46, this.world.map.title, {
      fontFamily: 'Arial, sans-serif', fontSize: '16px', color: '#9dc0e8', fontStyle: 'bold'
    }).setDepth(6);

    // Merge Tiled object positions with backend objects.
    // Tiled object "name" must match the backend MapObjectState "key".
    const mergedObjects = this.world.objects.map(obj => {
      const t = tiledObjects.find(o => o.name === obj.key);
      return (t?.x != null && t?.y != null) ? { ...obj, x: t.x, y: t.y } : obj;
    });

    mergedObjects.forEach(obj => this.createMarker(obj));
    this.createPlayer(this.world.player.x, this.world.player.y);
    this.refreshMarkerStates();
    this.updateNearestInteraction();
  }

  private renderCollisionZone(zone: CollisionZoneState) {
    const cx = zone.x + zone.width/2;
    const cy = zone.y + zone.height/2;
    const isDoor = /puerta|door/i.test(zone.label ?? '');
    // Walls: solid dark panels; doors get a subtle teal accent
    const themeClr = this.borderFor(this.world!.map.theme);
    this.add.rectangle(cx, cy, zone.width, zone.height,
      isDoor ? 0x0c1926 : 0x090d14, isDoor ? .92 : 1)
      .setStrokeStyle(isDoor ? 2 : 1, isDoor ? themeClr : 0x18263a, isDoor ? .8 : .45)
      .setDepth(4);
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
      if (isExit && this.textures.exists('dungeon-tiles')) {
        main = this.add.image(0, 0, 'dungeon-tiles', KenneyDungeonFrames.DOOR).setScale(2.5);
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

  private borderFor(theme: string): number {
    const m: Record<string, number> = {
      'protection-route':0x2f7476,'technical-record':0x2f5f8f,
      'risk-assessment':0x5b4f8f,'child-protection':0x5d9278,'follow-up':0x2f5f8f
    };
    return m[theme] ?? 0x4f7cac;
  }
}

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
        pixelArt: true,   // nearest-neighbour scaling — keeps pixel art sharp
        scale: { mode: Phaser.Scale.FIT, autoCenter: Phaser.Scale.CENTER_BOTH, width: 960, height: 540 },
        scene: this.scene
      });
    });
    window.setTimeout(() => { if (this.world()) this.scene?.setWorld(this.world()!); }, 0);
  }
}
