import { Component } from '@angular/core';

@Component({
  selector: 'app-velocities',
  standalone: true,
  template: `
    <div class="card placeholder">
      <h2>Velocities</h2>
      <p>This page will implement velocity calculations in Phase 4.</p>
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
export class Velocities {}