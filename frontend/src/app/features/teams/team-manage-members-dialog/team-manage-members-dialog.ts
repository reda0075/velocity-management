import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Modal } from '../../../shared/ui/modal/modal';
import { TeamApi } from '../../../core/services/team-api';
import { CollaboratorApi } from '../../../core/services/collaborator-api';
import { Collaborator } from '../../../core/models/collaborator';
import { Team } from '../../../core/models/team';
import { Toast } from '../../../core/services/toast';
import { extractErrorMessage } from '../../../core/utils/http-error';

@Component({
  selector: 'app-team-manage-members-dialog',
  standalone: true,
  imports: [CommonModule, Modal],
  templateUrl: './team-manage-members-dialog.html',
  styleUrl: './team-manage-members-dialog.scss'
})
export class TeamManageMembersDialog implements OnInit {
  @Input() teamId: number | null = null;
  @Input() teamName: string | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  private api = inject(TeamApi);
  private collaboratorApi = inject(CollaboratorApi);
  private toast = inject(Toast);

  currentMembers = signal<Collaborator[]>([]);
  availableCollaborators = signal<Collaborator[]>([]);
  submitting = signal(false);
  backendError = signal<string | null>(null);
  loading = signal(true);
  loadError = signal<string | null>(null);

  ngOnInit(): void {
    if (!this.teamId) return;
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.collaboratorApi.getAll().subscribe({
      next: (all) => {
        const active = all.filter(c => c.active);
        this.api.getMembers(this.teamId!).subscribe({
          next: (members) => {
            this.currentMembers.set(members);
            const memberIds = new Set(members.map(m => m.id));
            this.availableCollaborators.set(active.filter(c => !memberIds.has(c.id)));
            this.loading.set(false);
          },
          error: () => {
            this.loadError.set('Could not load team members.');
            this.loading.set(false);
          }
        });
      },
      error: () => {
        this.loadError.set('Could not load collaborators.');
        this.loading.set(false);
      }
    });
  }

  removeMember(collaborator: Collaborator): void {
    this.currentMembers.update(list => list.filter(c => c.id !== collaborator.id));
  }

  addMember(collaborator: Collaborator): void {
    this.currentMembers.update(list => [...list, collaborator]);
    this.availableCollaborators.update(list => list.filter(c => c.id !== collaborator.id));
  }

  submit(): void {
    if (!this.teamId) return;
    this.backendError.set(null);
    this.submitting.set(true);
    const payload = { collaboratorIds: this.currentMembers().map(c => c.id) };
    this.api.updateMembers(this.teamId!, payload).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success(`${this.teamName || 'Team'} members updated.`);
        this.saved.emit();
        this.closed.emit();
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.backendError.set(
          extractErrorMessage(err, 'Could not update team members. Please try again.')
        );
      }
    });
  }
}
