import { Component, inject, signal } from '@angular/core';
import { VelocityApi } from '../../core/services/velocity-api';
import { Velocity } from '../../core/models/velocity';

@Component({
  selector: 'app-my-velocities',
  standalone: true,
  template: `
    <section class="card page-card">
      <h2>My Velocities</h2>
      @if (error()) {
        <p>{{ error() }}</p>
      } @else if (velocities().length) {
        <table>
          <thead><tr><th>Period</th><th>Velocity</th><th>Status</th></tr></thead>
          <tbody>
            @for (velocity of velocities(); track velocity.id) {
              <tr><td>{{ velocity.month }}/{{ velocity.year }}</td><td>{{ velocity.velocity }}</td><td>{{ velocity.status }}</td></tr>
            }
          </tbody>
        </table>
      } @else {
        <p>No velocity history is available.</p>
      }
    </section>
  `,
  styles: ['.page-card { padding: var(--space-6); } table { width: 100%; border-collapse: collapse; margin-top: var(--space-4); } th, td { text-align: left; padding: var(--space-2); border-bottom: 1px solid var(--color-border); }']
})
export class MyVelocities {
  private api = inject(VelocityApi);
  velocities = signal<Velocity[]>([]);
  error = signal<string | null>(null);

  constructor() {
    this.api.getMine().subscribe({
      next: velocities => this.velocities.set(velocities),
      error: () => this.error.set('Your velocity history could not be loaded.')
    });
  }
}
