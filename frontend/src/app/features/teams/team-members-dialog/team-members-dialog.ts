import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Modal } from '../../../shared/ui/modal/modal';
import { TeamApi } from '../../../core/services/team-api';
import { Collaborator } from '../../../core/models/collaborator';

@Component({
  selector: 'app-team-members-dialog',
  standalone: true,
  imports: [CommonModule, Modal],
  templateUrl: './team-members-dialog.html',
  styleUrl: './team-members-dialog.scss'
})
export class TeamMembersDialog implements OnInit {
  @Input() teamId: number | null = null;
  @Input() teamName: string | null = null;
  @Output() closed = new EventEmitter<void>();

  private api = inject(TeamApi);

  members = signal<Collaborator[]>([]);
  loading = signal(true);
  loadError = signal<string | null>(null);

  ngOnInit(): void {
    if (!this.teamId) return;
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.api.getMembers(this.teamId!).subscribe({
      next: (data) => {
        this.members.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Could not load team members.');
        this.loading.set(false);
      }
    });
  }
}
