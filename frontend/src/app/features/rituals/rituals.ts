import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  template: `
    <div class="card placeholder">
      <h2>Dashboard</h2>
      <p>This page will show collaborator, ritual, and velocity summaries in Phase 5.</p>
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