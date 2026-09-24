import { Component, inject, signal } from '@angular/core';
import { CollaboratorApi } from '../../core/services/collaborator-api';
import { Collaborator } from '../../core/models/collaborator';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  template: `
    <section class="card page-card">
      <h2>My Profile</h2>
      @if (profile(); as collaborator) {
        <dl>
          <dt>Name</dt><dd>{{ collaborator.firstName }} {{ collaborator.lastName }}</dd>
          <dt>Matricule</dt><dd>{{ collaborator.matricule }}</dd>
          <dt>Profile</dt><dd>{{ collaborator.profile }}</dd>
        </dl>
      } @else if (error()) {
        <p>{{ error() }}</p>
      } @else {
        <p>Loading profile…</p>
      }
    </section>
  `,
  styles: ['.page-card { padding: var(--space-6); } dt { font-weight: 600; margin-top: var(--space-3); } dd { margin: var(--space-1) 0; }']
})
export class MyProfile {
  private api = inject(CollaboratorApi);
  profile = signal<Collaborator | null>(null);
  error = signal<string | null>(null);

  constructor() {
    this.api.getMine().subscribe({
      next: profile => this.profile.set(profile),
      error: () => this.error.set('Your profile could not be loaded.')
    });
  }
}
