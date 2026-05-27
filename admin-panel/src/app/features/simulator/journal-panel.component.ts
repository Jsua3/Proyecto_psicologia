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
