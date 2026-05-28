/**
 * GameFeelSystem — Fase 3: Golden Vertical Slice
 *
 * Sub-pixel movement, input buffering (~120ms), acceleration/friction,
 * idle/walk animations by direction, camera smoothing, screen-shake,
 * particles. All animations respect prefers-reduced-motion.
 *
 * Designed to plug into the existing GameWorldComponent (Phaser host).
 * Coordinates in pixels (coherent with WorldDefinition / V6+V7).
 */
import Phaser from 'phaser';

// ─── Configuration ───────────────────────────────────────────────────────────

export interface GameFeelConfig {
  /** Player acceleration (px/frame^2). Default: 600 */
  acceleration: number;
  /** Friction deceleration (px/frame^2). Default: 800 */
  friction: number;
  /** Max speed (px/s). Default: 160 */
  maxSpeed: number;
  /** Input buffer window in ms. Default: 120 */
  inputBufferMs: number;
  /** Camera lerp factor (0–1). Default: 0.08 */
  cameraLerp: number;
  /** Whether user prefers reduced motion (disables shake/particles). */
  reducedMotion: boolean;
}

const DEFAULT_CONFIG: GameFeelConfig = {
  acceleration: 600,
  friction: 800,
  maxSpeed: 160,
  inputBufferMs: 120,
  cameraLerp: 0.08,
  reducedMotion: false,
};

// ─── Input Buffer ────────────────────────────────────────────────────────────

interface BufferedInput {
  direction: 'up' | 'down' | 'left' | 'right';
  timestamp: number;
}

// ─── System ──────────────────────────────────────────────────────────────────

export class GameFeelSystem {
  private config: GameFeelConfig;
  private inputBuffer: BufferedInput[] = [];
  private shakeTimer = 0;
  private shakeIntensity = 0;

  constructor(config: Partial<GameFeelConfig> = {}) {
    this.config = { ...DEFAULT_CONFIG, ...config };
  }

  // ─── Input Buffering ─────────────────────────────────────────────────────

  /** Queue a directional input. Called from keyboard/touch handlers. */
  bufferInput(direction: BufferedInput['direction']): void {
    this.inputBuffer.push({ direction, timestamp: Date.now() });
  }

  /** Consume and return the most recent valid buffered input. */
  consumeBufferedInput(): BufferedInput['direction'] | null {
    const now = Date.now();
    // Purge expired inputs
    this.inputBuffer = this.inputBuffer.filter(
      i => now - i.timestamp <= this.config.inputBufferMs
    );
    if (this.inputBuffer.length === 0) return null;
    const input = this.inputBuffer.pop()!;
    this.inputBuffer = [];
    return input.direction;
  }

  // ─── Sub-pixel Movement ──────────────────────────────────────────────────

  /**
   * Apply acceleration/friction to the player sprite.
   * Call in the Phaser scene's `update()` with active cursors.
   */
  applyMovement(
    body: Phaser.Physics.Arcade.Body,
    cursors: { up: boolean; down: boolean; left: boolean; right: boolean },
    delta: number
  ): void {
    const { acceleration, friction, maxSpeed } = this.config;
    const dt = delta / 1000;

    let ax = 0;
    let ay = 0;
    if (cursors.left) ax -= acceleration;
    if (cursors.right) ax += acceleration;
    if (cursors.up) ay -= acceleration;
    if (cursors.down) ay += acceleration;

    // Apply acceleration or friction
    if (ax !== 0) {
      body.velocity.x += ax * dt;
    } else {
      body.velocity.x = this.applyFriction(body.velocity.x, friction * dt);
    }
    if (ay !== 0) {
      body.velocity.y += ay * dt;
    } else {
      body.velocity.y = this.applyFriction(body.velocity.y, friction * dt);
    }

    // Clamp to max speed
    body.velocity.x = Phaser.Math.Clamp(body.velocity.x, -maxSpeed, maxSpeed);
    body.velocity.y = Phaser.Math.Clamp(body.velocity.y, -maxSpeed, maxSpeed);
  }

  private applyFriction(velocity: number, frictionDelta: number): number {
    if (Math.abs(velocity) < frictionDelta) return 0;
    return velocity > 0 ? velocity - frictionDelta : velocity + frictionDelta;
  }

  // ─── Camera Smoothing ───────────────────────────────────────────────────

  /** Smoothly follow the player with lerp and map bounds. */
  updateCamera(
    camera: Phaser.Cameras.Scene2D.Camera,
    target: { x: number; y: number },
    mapWidth: number,
    mapHeight: number
  ): void {
    camera.startFollow(target as Phaser.GameObjects.GameObject, true,
      this.config.cameraLerp, this.config.cameraLerp);
    camera.setBounds(0, 0, mapWidth, mapHeight);
  }

  // ─── Screen Shake ────────────────────────────────────────────────────────

  /**
   * Trigger a parametric screen shake.
   * Respects prefers-reduced-motion (skips if enabled).
   */
  shake(camera: Phaser.Cameras.Scene2D.Camera, intensity = 0.005, duration = 150): void {
    if (this.config.reducedMotion) return;
    camera.shake(duration, intensity);
  }

  // ─── Direction from Velocity ─────────────────────────────────────────────

  /** Determine facing direction from velocity for animation selection. */
  facingDirection(vx: number, vy: number): 'up' | 'down' | 'left' | 'right' | 'idle' {
    if (Math.abs(vx) < 1 && Math.abs(vy) < 1) return 'idle';
    if (Math.abs(vx) > Math.abs(vy)) {
      return vx > 0 ? 'right' : 'left';
    }
    return vy > 0 ? 'down' : 'up';
  }

  // ─── Particles (placeholder for art pipeline) ────────────────────────────

  /**
   * Emit a burst of particles at a position.
   * No-op if reducedMotion is enabled.
   * Implementation depends on Phaser particle emitter setup.
   */
  emitParticles(
    scene: Phaser.Scene,
    x: number,
    y: number,
    _config?: { count?: number; color?: number; lifespan?: number }
  ): void {
    if (this.config.reducedMotion) return;
    // Placeholder: actual particle emitter will be configured per-scene
    // when sprite/art assets are available.
    // scene.add.particles(x, y, 'particle-key', { ... });
  }
}
