import { Component } from '@angular/core';

@Component({
  selector: 'app-rituals',
  standalone: true,
  template: `
    <div class="card placeholder">
      <h2>Rituals</h2>
      <p>This page will implement ritual CRUD in Phase 3.</p>
    </div>
  `,
  styles: [`
    .placeholder {
      padding: var(--space-6);
      color: var(--color-text-muted);
    }
    h2 { margin-bottom: var(--space-2); }
  `]
})
export class Rituals {}