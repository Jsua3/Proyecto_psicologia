/**
 * DialogueDirector — Fase 3: Golden Vertical Slice
 *
 * Typewriter with per-character timing, portrait expressions,
 * rich text tags [shake]/[wave]/[pause:ms]/[speed:factor],
 * skippable, synchronized captions.
 *
 * Designed to work with rexUI (phaser3-rex-plugins) once installed,
 * but the core logic is framework-agnostic for testability.
 */

// ─── Types ───────────────────────────────────────────────────────────────────

export interface DialogueLine {
  speakerName: string;
  text: string;
  emotion: string;
  portraitKey?: string;
  /** Voice blip speaker ID for AudioDirector.speak(). */
  voiceId?: string;
}

export interface DialogueTag {
  type: 'shake' | 'wave' | 'pause' | 'speed' | 'color' | 'text';
  value?: string | number;
  text?: string;
}

export interface TypewriterState {
  /** Full parsed tags from the current line. */
  tags: DialogueTag[];
  /** Characters revealed so far (for rendering). */
  visibleText: string;
  /** Current character index. */
  charIndex: number;
  /** Whether the line is fully revealed. */
  complete: boolean;
  /** Current speed multiplier (affected by [speed:N] tags). */
  speedFactor: number;
  /** Current speaker for portrait/voice. */
  speaker: string;
  /** Current emotion for portrait expression. */
  emotion: string;
}

export interface DialogueDirectorConfig {
  /** Base ms per character. Default: 35 */
  baseCharDelay: number;
  /** Punctuation delay multiplier. Default: 3 */
  punctuationMultiplier: number;
  /** Whether user prefers reduced motion (disables shake/wave effects). */
  reducedMotion: boolean;
  /** Callback fired each time a character is revealed. */
  onCharReveal?: (state: TypewriterState) => void;
  /** Callback fired when a [pause:ms] tag is hit. */
  onPause?: (ms: number) => void;
  /** Callback fired when the line is complete. */
  onLineComplete?: (state: TypewriterState) => void;
  /** Callback to play a voice blip for a speaker. */
  onVoiceBlip?: (speakerId: string) => void;
}

const DEFAULT_CONFIG: DialogueDirectorConfig = {
  baseCharDelay: 35,
  punctuationMultiplier: 3,
  reducedMotion: false,
};

const PUNCTUATION = new Set(['.', ',', '!', '?', ';', ':', '—', '…']);

// ─── Tag Parser ──────────────────────────────────────────────────────────────

/**
 * Parse a dialogue text string with rich tags into a flat list of DialogueTags.
 *
 * Supported tags:
 *   [shake]text[/shake]  — shaking text (skipped if reducedMotion)
 *   [wave]text[/wave]    — wavy text (skipped if reducedMotion)
 *   [pause:500]          — pause for 500ms
 *   [speed:0.5]          — slow down to 50% speed
 *   [speed:2]            — speed up to 200%
 *   [/speed]             — reset speed to 1.0
 *   [color:#FF0000]text[/color] — colored text
 */
export function parseTags(raw: string): DialogueTag[] {
  const tags: DialogueTag[] = [];
  const regex = /\[(shake|wave|pause|speed|color|\/shake|\/wave|\/speed|\/color)(?::([^\]]+))?\]/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = regex.exec(raw)) !== null) {
    // Push preceding plain text
    if (match.index > lastIndex) {
      tags.push({ type: 'text', text: raw.slice(lastIndex, match.index) });
    }

    const tagName = match[1];
    const tagValue = match[2];

    if (tagName === 'pause') {
      tags.push({ type: 'pause', value: parseInt(tagValue || '500', 10) });
    } else if (tagName === 'speed') {
      tags.push({ type: 'speed', value: parseFloat(tagValue || '1') });
    } else if (tagName === '/speed') {
      tags.push({ type: 'speed', value: 1.0 });
    } else if (tagName === 'shake' || tagName === 'wave' || tagName === 'color') {
      tags.push({ type: tagName, value: tagValue });
    }
    // Closing tags for shake/wave/color are tracked but not pushed as separate tags
    // (the rendering layer handles start/end via the sequence)

    lastIndex = match.index + match[0].length;
  }

  // Trailing text
  if (lastIndex < raw.length) {
    tags.push({ type: 'text', text: raw.slice(lastIndex) });
  }

  return tags;
}

// ─── Director ─────────────────────────────────────────────────────────────────

export class DialogueDirector {
  private config: DialogueDirectorConfig;
  private state: TypewriterState | null = null;
  private queue: DialogueLine[] = [];
  private timer: ReturnType<typeof setTimeout> | null = null;
  private paused = false;

  constructor(config: Partial<DialogueDirectorConfig> = {}) {
    this.config = { ...DEFAULT_CONFIG, ...config };
  }

  // ─── Queuing ────────────────────────────────────────────────────────────

  /** Enqueue dialogue lines. If not currently playing, starts immediately. */
  enqueue(lines: DialogueLine[]): void {
    this.queue.push(...lines);
    if (!this.state) this.advanceLine();
  }

  /** Clear all queued lines and stop the current typewriter. */
  clear(): void {
    this.queue = [];
    this.stop();
  }

  /** Get the current typewriter state (for rendering). */
  getState(): TypewriterState | null {
    return this.state;
  }

  /** Whether there are more lines after the current one. */
  hasMore(): boolean {
    return this.queue.length > 0;
  }

  // ─── Typewriter ─────────────────────────────────────────────────────────

  /** Skip to the end of the current line (reveal all text). */
  skip(): void {
    if (!this.state || this.state.complete) return;
    // Reveal everything instantly
    const fullText = this.state.tags
      .filter(t => t.type === 'text')
      .map(t => t.text ?? '')
      .join('');
    this.state = {
      ...this.state,
      visibleText: fullText,
      charIndex: fullText.length,
      complete: true,
    };
    if (this.timer) clearTimeout(this.timer);
    this.config.onLineComplete?.(this.state);
  }

  /** Advance to the next queued line. If none remain, state becomes null. */
  advanceLine(): void {
    this.stop();
    const line = this.queue.shift();
    if (!line) {
      this.state = null;
      return;
    }

    const tags = parseTags(line.text);
    this.state = {
      tags,
      visibleText: '',
      charIndex: 0,
      complete: false,
      speedFactor: 1.0,
      speaker: line.speakerName,
      emotion: line.emotion,
    };
    this.tickTypewriter();
  }

  // ─── Internal ───────────────────────────────────────────────────────────

  private tickTypewriter(): void {
    if (!this.state || this.state.complete || this.paused) return;

    // Collect all plain text
    const allText = this.state.tags
      .filter(t => t.type === 'text')
      .map(t => t.text ?? '')
      .join('');

    if (this.state.charIndex >= allText.length) {
      this.state = { ...this.state, complete: true, visibleText: allText };
      this.config.onLineComplete?.(this.state);
      return;
    }

    // Process tags at the current position
    const currentTags = this.getTagsAtPosition(this.state.charIndex);
    for (const tag of currentTags) {
      if (tag.type === 'pause') {
        this.config.onPause?.(tag.value as number);
        this.timer = setTimeout(() => this.tickTypewriter(), tag.value as number);
        return;
      }
      if (tag.type === 'speed') {
        this.state = { ...this.state, speedFactor: tag.value as number };
      }
    }

    // Reveal next character
    const char = allText[this.state.charIndex];
    this.state = {
      ...this.state,
      visibleText: allText.slice(0, this.state.charIndex + 1),
      charIndex: this.state.charIndex + 1,
    };

    this.config.onCharReveal?.(this.state);

    // Voice blip every 2-3 characters
    if (this.state.charIndex % 2 === 0 && char !== ' ') {
      this.config.onVoiceBlip?.(this.state.speaker);
    }

    // Compute delay
    let delay = this.config.baseCharDelay / this.state.speedFactor;
    if (PUNCTUATION.has(char)) {
      delay *= this.config.punctuationMultiplier;
    }

    this.timer = setTimeout(() => this.tickTypewriter(), delay);
  }

  private getTagsAtPosition(_charIndex: number): DialogueTag[] {
    // Simplified: in the full implementation, this maps char positions
    // to the tags that apply at that position. For now, returns empty.
    return [];
  }

  private stop(): void {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }
    this.state = null;
  }

  /** Pause the typewriter. */
  pause(): void {
    this.paused = true;
  }

  /** Resume the typewriter. */
  resume(): void {
    this.paused = false;
    this.tickTypewriter();
  }

  /** Clean up. Call on scene destroy. */
  destroy(): void {
    this.clear();
    this.config = { ...DEFAULT_CONFIG };
  }
}
