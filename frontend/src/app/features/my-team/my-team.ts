import { Component, inject, signal } from '@angular/core';
import { TeamApi } from '../../core/services/team-api';
import { Team } from '../../core/models/team';

@Component({
  selector: 'app-my-team',
  standalone: true,
  template: `
    <section class="card page-card">
      <h2>My Team</h2>
      @if (team(); as currentTeam) {
        <h3>{{ currentTeam.name }}</h3>
        <p>{{ currentTeam.description || 'No team description.' }}</p>
      } @else if (error()) {
        <p>{{ error() }}</p>
      } @else {
        <p>Loading team…</p>
      }
    </section>
  `,
  styles: ['.page-card { padding: var(--space-6); } h3 { margin: var(--space-3) 0 var(--space-2); }']
})
export class MyTeam {
  private api = inject(TeamApi);
  team = signal<Team | null>(null);
  error = signal<string | null>(null);

  constructor() {
    this.api.getMine().subscribe({
      next: team => this.team.set(team),
      error: () => this.error.set('No team is currently assigned to your profile.')
    });
  }
}
