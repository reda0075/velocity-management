import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  template: `
    <div class="kpi-card">
      <span class="kpi-card__label">{{ label }}</span>
      <span class="kpi-card__value" [class.kpi-card__value--muted]="value === '—'">{{ value }}</span>
      @if (hint) {
        <span class="kpi-card__hint">{{ hint }}</span>
      }
    </div>
  `,
  styles: [`
    .kpi-card {
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: var(--space-4);
      display: flex;
      flex-direction: column;
      gap: var(--space-1);
      min-width: 0;
    }

    .kpi-card__label {
      font-size: 12px;
      font-weight: 600;
      color: var(--color-text-muted);
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .kpi-card__value {
      font-family: var(--font-display);
      font-size: 28px;
      font-weight: 700;
      color: var(--color-text);
      font-variant-numeric: tabular-nums;
    }

    .kpi-card__value--muted {
      color: var(--color-text-faint);
    }

    .kpi-card__hint {
      font-size: 12px;
      color: var(--color-text-faint);
    }
  `]
})
export class KpiCard {
  @Input() label = '';
  @Input() value: string | number = '—';
  @Input() hint = '';
}