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

  allCollaborators = signal<Collaborator[]>([]);
  currentMembers = signal<Collaborator[]>([]);
  selectedIds = signal<Set<number>>(new Set());
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
        this.allCollaborators.set(active);
        this.api.getMembers(this.teamId!).subscribe({
          next: (members) => {
            this.currentMembers.set(members);
            const selected = new Set<number>(members.map(m => m.id));
            this.selectedIds.set(selected);
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

  toggle(id: number): void {
    this.selectedIds.update(set => {
      const next = new Set(set);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  getTeamName(teamId: number | null): string | null {
    if (!teamId) return null;
    const all = this.allCollaborators();
    const c = all.find(x => x.id === teamId);
    return c?.teamName ?? null;
  }

  submit(): void {
    if (!this.teamId) return;
    this.backendError.set(null);
    this.submitting.set(true);
    const payload = { collaboratorIds: Array.from(this.selectedIds()) };
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
