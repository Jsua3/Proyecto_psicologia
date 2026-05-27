import { CommonModule } from '@angular/common';
import { AfterViewChecked, Component, ElementRef, OnDestroy, ViewChild, effect, inject, input, output, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { DialogueChoiceState, DialogueState, MapObjectState } from '../../core/models/simulation.model';
import { AudioService } from './audio.service';

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
        role="dialog"
        aria-modal="false"
        [attr.aria-label]="d.speakerName + ': ' + fullText()"
        aria-live="polite">

        <!-- Portrait -->
        <div class="portrait" aria-hidden="true">
          <span class="portrait-emoji">{{ d.portraitKey ?? '🧑‍⚕️' }}</span>
          @if (d.emotion && d.emotion !== 'neutral') {
            <span class="emotion-chip">{{ emotionEmoji(d.emotion) }}</span>
          }
        </div>

        <!-- Text area -->
        <div class="strip-body">
          <p class="speaker-name">{{ d.speakerName }}</p>
          <p class="dialogue-text" aria-live="polite">{{ displayedText() }}<span class="cursor" [class.cursor--done]="isTypingComplete()" aria-hidden="true">▋</span></p>

          @if (isTypingComplete() && d.choices?.length) {
            <div class="choices" role="group" aria-label="Opciones de intervención">
              @for (choice of d.choices; track choice.key) {
                <button
                  type="button"
                  class="choice-btn"
                  [class.choice-btn--recommended]="choice.isRecommended"
                  [class.choice-btn--prohibited]="choice.isProhibited"
                  [attr.aria-label]="choice.text + (choice.isRecommended ? ' (recomendada)' : '') + (choice.isProhibited ? ' (contraindicada)' : '')"
                  (mouseenter)="onChoiceHover()"
                  (click)="handleChoice(choice)">
                  {{ choice.text }}
                </button>
              }
            </div>
          }

          @if (isTypingComplete() && !d.choices?.length) {
            <button type="button" class="close-btn psy-button psy-button--ghost" (click)="close.emit()" aria-label="Cerrar diálogo (Esc)">
              Continuar <span aria-hidden="true">▶</span>
            </button>
          }
        </div>
      </div>
    }
  `,
  styles: [`
    .dialogue-strip {
      width: 100%;
      min-height: 110px;
      display: flex;
      align-items: flex-start;
      gap: 0;
      padding: 0;
      background: rgba(8,12,18,.95);
      border-top: 2px solid rgba(79,163,165,.35);
      backdrop-filter: blur(16px) saturate(110%);
      animation: strip-rise 160ms cubic-bezier(.2,.8,.2,1) both;
    }
    .strip--warning {
      border-top-color: rgba(168,80,98,.6);
    }
    .strip--supervisory {
      border-top-color: rgba(79,163,100,.6);
      background: rgba(8,14,10,.95);
    }

    .portrait {
      position: relative;
      flex-shrink: 0;
      width: 80px;
      min-height: 110px;
      display: grid;
      place-items: center;
      border-right: 1px solid rgba(79,163,165,.2);
      background: rgba(79,163,165,.06);
    }
    .portrait-emoji { font-size: 2.2rem; line-height: 1; }
    .emotion-chip {
      position: absolute;
      bottom: 6px;
      right: 6px;
      font-size: .9rem;
      background: rgba(8,12,18,.7);
      border-radius: 50%;
      padding: 2px 3px;
    }

    .strip-body {
      flex: 1;
      padding: 14px 18px 14px 16px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      min-width: 0;
    }
    .speaker-name {
      margin: 0;
      font-size: .7rem;
      font-weight: 900;
      letter-spacing: .12em;
      text-transform: uppercase;
      color: #4fa3a5;
    }
    .strip--supervisory .speaker-name { color: #5d9278; }
    .strip--warning .speaker-name { color: #a85062; }

    .dialogue-text {
      margin: 0;
      font-size: .92rem;
      line-height: 1.55;
      color: rgba(232,240,244,.92);
      min-height: 1.55em;
    }
    .cursor {
      display: inline-block;
      animation: blink .6s step-end infinite;
      color: #4fa3a5;
      margin-left: 1px;
    }
    .cursor--done { display: none; }

    .choices {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 4px;
    }
    .choice-btn {
      padding: 7px 16px;
      border-radius: 999px;
      border: 1px solid rgba(79,163,165,.35);
      background: rgba(79,163,165,.08);
      color: rgba(232,240,244,.88);
      font: inherit;
      font-size: .82rem;
      cursor: pointer;
      transition: border-color 140ms ease, background 140ms ease;
    }
    .choice-btn:hover {
      border-color: rgba(79,163,165,.65);
      background: rgba(79,163,165,.18);
    }
    .choice-btn--recommended {
      border-color: rgba(59,130,246,.5);
      background: rgba(59,130,246,.1);
      color: rgba(147,197,253,.95);
    }
    .choice-btn--recommended:hover { border-color: rgba(59,130,246,.8); background: rgba(59,130,246,.2); }
    .choice-btn--prohibited {
      border-color: rgba(168,80,98,.45);
      background: rgba(168,80,98,.08);
      color: rgba(252,165,165,.88);
    }
    .choice-btn--prohibited:hover { border-color: rgba(168,80,98,.7); }

    .close-btn {
      align-self: flex-end;
      font-size: .8rem;
      padding: 5px 14px;
      min-height: 34px;
    }

    @keyframes strip-rise {
      from { transform: translateY(100%); opacity: 0; }
      to   { transform: translateY(0);    opacity: 1; }
    }
    @keyframes blink {
      0%, 100% { opacity: 1; }
      50%       { opacity: 0; }
    }
    @media (max-width: 560px) {
      .portrait { width: 56px; }
      .strip-body { padding: 10px 12px; }
    }
    @media (prefers-reduced-motion: reduce) {
      .dialogue-strip { animation: none; }
      .cursor { animation: none; }
    }
  `]
})
export class DialoguePanelComponent implements AfterViewChecked, OnDestroy {
  readonly dialogue    = input<DialogueState | null>(null);
  readonly interaction = input<MapObjectState | null>(null);

  readonly close   = output<void>();
  readonly execute = output<number>();
  readonly useTool = output<string>();

  @ViewChild('dialogueBox') private dialogueBox?: ElementRef<HTMLDivElement>;

  readonly displayedText    = signal('');
  readonly isTypingComplete = signal(false);

  private typewriterHandle: ReturnType<typeof setInterval> | null = null;
  private currentLineIndex = 0;
  private readonly audio = inject(AudioService);

  constructor() {
    effect(() => {
      const d = this.dialogue();
      this.stopTypewriter();
      this.currentLineIndex = 0;
      if (d?.lines?.length) {
        this.audio.play('dialogue-open');
        this.startTypewriter(d.lines[0].text);
      } else {
        this.displayedText.set('');
        this.isTypingComplete.set(true);
      }
    });
  }

  onChoiceHover(): void {
    this.audio.play('choice-hover');
  }

  ngAfterViewChecked() {
    // Ensure dialogue box is scrolled to bottom when new text renders
    if (this.dialogueBox) {
      const el = this.dialogueBox.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }

  ngOnDestroy() {
    this.stopTypewriter();
  }

  handleChoice(choice: DialogueChoiceState) {
    this.audio.play(choice.isProhibited ? 'choice-error' : 'choice-select');
    if (choice.decisionOptionId != null) {
      this.execute.emit(choice.decisionOptionId);
    } else if (choice.requiredToolCode != null) {
      this.useTool.emit(choice.requiredToolCode);
    }
  }

  fullText(): string {
    return this.dialogue()?.lines?.map(l => l.text).join(' ') ?? '';
  }

  emotionEmoji(emotion: string): string {
    const map: Record<string, string> = {
      positive: '😊', negative: '😔', anxious: '😰',
      angry: '😠', danger: '⚠️', neutral: ''
    };
    return map[emotion] ?? '';
  }

  private startTypewriter(text: string) {
    this.displayedText.set('');
    this.isTypingComplete.set(false);
    let pos = 0;
    this.typewriterHandle = setInterval(() => {
      pos++;
      this.displayedText.set(text.slice(0, pos));
      if (pos >= text.length) {
        this.stopTypewriter();
        this.isTypingComplete.set(true);
      }
    }, TYPEWRITER_INTERVAL_MS);
  }

  private stopTypewriter() {
    if (this.typewriterHandle !== null) {
      clearInterval(this.typewriterHandle);
      this.typewriterHandle = null;
    }
  }
}
