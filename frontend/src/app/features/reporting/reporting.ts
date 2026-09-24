import { Component } from '@angular/core';

@Component({
  selector: 'app-reporting',
  standalone: true,
  template: `
    <section class="card page-card">
      <h2>Reporting</h2>
      <p>Reporting is restricted to administrators. Report generation and export remain enforced by the backend.</p>
    </section>
  `,
  styles: ['.page-card { padding: var(--space-6); }']
})
export class Reporting {}
