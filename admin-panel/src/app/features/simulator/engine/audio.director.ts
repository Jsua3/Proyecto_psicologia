/**
 * AudioDirector — Fase 3: Golden Vertical Slice
 *
 * Music by layers/stems with crossfade driven by stressIndex.
 * SFX per interaction type. "Voice blips" per character.
 * API: setStress(0..100), playSfx(key), speak(speakerId).
 *
 * Uses Howler.js for Web Audio with fallback.
 * All audio respects prefers-reduced-motion (muted by default until
 * user interaction, per browser autoplay policy).
 */

// ─── Types ───────────────────────────────────────────────────────────────────

export interface AudioConfig {
  /** Master volume 0–1. Default: 0.7 */
  masterVolume: number;
  /** Music volume 0–1. Default: 0.4 */
  musicVolume: number;
  /** SFX volume 0–1. Default: 0.6 */
  sfxVolume: number;
  /** Crossfade duration in ms. Default: 2000 */
  crossfadeMs: number;
  /** Whether user prefers reduced motion (disables dynamic audio effects). */
  reducedMotion: boolean;
  /** Whether audio is globally muted (default until first user interaction). */
  muted: boolean;
}

export interface MusicLayer {
  /** Identifier for this layer (e.g. 'calm', 'tension', 'crisis'). */
  key: string;
  /** URL or sprite key for the audio source. */
  src: string;
  /** Stress range [min, max] where this layer is audible (0–100). */
  stressRange: [number, number];
}

export interface SfxDefinition {
  key: string;
  src: string;
  /** Optional sprite definition: [offset_ms, duration_ms]. */
  sprite?: [number, number];
}

export interface VoiceBlip {
  /** Character/speaker identifier. */
  speakerId: string;
  /** Short blip sound URL. */
  src: string;
  /** Pitch variation range [min, max] multiplier. Default: [0.9, 1.1]. */
  pitchRange?: [number, number];
}

// ─── Director ─────────────────────────────────────────────────────────────────

const DEFAULT_CONFIG: AudioConfig = {
  masterVolume: 0.7,
  musicVolume: 0.4,
  sfxVolume: 0.6,
  crossfadeMs: 2000,
  reducedMotion: false,
  muted: true, // muted by default; unmuted on first user gesture
};

export class AudioDirector {
  private config: AudioConfig;
  private currentStress = 0;
  private initialized = false;
  private musicLayers: Map<string, MusicLayer> = new Map();
  private sfxLibrary: Map<string, SfxDefinition> = new Map();
  private voiceBlips: Map<string, VoiceBlip> = new Map();

  // Howl instances would be stored here once Howler is imported.
  // Typed as `any` during skeleton phase to avoid hard dependency.
  private musicHowls: Map<string, unknown> = new Map();
  private sfxHowls: Map<string, unknown> = new Map();
  private blipHowls: Map<string, unknown> = new Map();

  constructor(config: Partial<AudioConfig> = {}) {
    this.config = { ...DEFAULT_CONFIG, ...config };
  }

  // ─── Initialization ─────────────────────────────────────────────────────

  /**
   * Register music layers, SFX, and voice blips.
   * Call once when the scene loads.
   * Actual Howl creation deferred to when Howler.js assets are available.
   */
  registerMusicLayers(layers: MusicLayer[]): void {
    layers.forEach(l => this.musicLayers.set(l.key, l));
  }

  registerSfx(definitions: SfxDefinition[]): void {
    definitions.forEach(d => this.sfxLibrary.set(d.key, d));
  }

  registerVoiceBlips(blips: VoiceBlip[]): void {
    blips.forEach(b => this.voiceBlips.set(b.speakerId, b));
  }

  /** Call on first user interaction to enable audio (browser autoplay policy). */
  unlock(): void {
    this.config.muted = false;
    this.initialized = true;
    // Resume Howler context if needed:
    // Howler.ctx?.resume();
  }

  // ─── Stress-driven Music ────────────────────────────────────────────────

  /**
   * Set the current stress index (0–100).
   * Layers whose stressRange includes this value will fade in;
   * others will fade out. Crossfade over config.crossfadeMs.
   */
  setStress(stress: number): void {
    this.currentStress = Math.max(0, Math.min(100, stress));
    if (!this.initialized || this.config.muted) return;

    // For each registered layer, compute target volume
    this.musicLayers.forEach((layer, key) => {
      const [min, max] = layer.stressRange;
      const inRange = this.currentStress >= min && this.currentStress <= max;
      const targetVolume = inRange
        ? this.config.musicVolume * this.config.masterVolume
        : 0;

      // Placeholder: actual Howl fade call
      // const howl = this.musicHowls.get(key);
      // if (howl) howl.fade(howl.volume(), targetVolume, this.config.crossfadeMs);
    });
  }

  getStress(): number {
    return this.currentStress;
  }

  // ─── SFX ────────────────────────────────────────────────────────────────

  /** Play a one-shot SFX by key. */
  playSfx(key: string): void {
    if (!this.initialized || this.config.muted) return;
    const def = this.sfxLibrary.get(key);
    if (!def) return;

    // Placeholder: actual Howl play call
    // const howl = this.sfxHowls.get(key);
    // if (howl) howl.play();
  }

  // ─── Voice Blips ────────────────────────────────────────────────────────

  /**
   * Play a short blip sound for a character (typewriter "voice").
   * Slight random pitch variation for organic feel.
   */
  speak(speakerId: string): void {
    if (!this.initialized || this.config.muted) return;
    const blip = this.voiceBlips.get(speakerId);
    if (!blip) return;

    const [minPitch, maxPitch] = blip.pitchRange ?? [0.9, 1.1];
    const _rate = minPitch + Math.random() * (maxPitch - minPitch);

    // Placeholder: actual Howl play with rate
    // const howl = this.blipHowls.get(speakerId);
    // if (howl) { howl.rate(rate); howl.play(); }
  }

  // ─── Lifecycle ──────────────────────────────────────────────────────────

  /** Mute all audio. */
  mute(): void {
    this.config.muted = true;
  }

  /** Unmute all audio. */
  unmute(): void {
    this.config.muted = false;
  }

  /** Stop all audio and free resources. Call on scene destroy. */
  destroy(): void {
    this.musicHowls.forEach((_h: unknown) => {
      // (h as Howl).stop(); (h as Howl).unload();
    });
    this.sfxHowls.forEach((_h: unknown) => {
      // (h as Howl).stop(); (h as Howl).unload();
    });
    this.blipHowls.forEach((_h: unknown) => {
      // (h as Howl).stop(); (h as Howl).unload();
    });
    this.musicHowls.clear();
    this.sfxHowls.clear();
    this.blipHowls.clear();
    this.initialized = false;
  }
}
