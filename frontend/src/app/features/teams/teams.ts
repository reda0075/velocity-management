import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TeamApi } from '../../core/services/team-api';
import { Team } from '../../core/models/team';
import { Toast } from '../../core/services/toast';
import { DataRefreshService } from '../../core/services/data-refresh';
import { extractErrorMessage } from '../../core/utils/http-error';
import { TeamFormDialog } from './team-form-dialog/team-form-dialog';
import { TeamManageMembersDialog } from './team-manage-members-dialog/team-manage-members-dialog';
import { ConfirmDialog } from '../../shared/dialogs/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-teams',
  standalone: true,
  imports: [TeamFormDialog, TeamManageMembersDialog, ConfirmDialog],
  templateUrl: './teams.html',
  styleUrl: './teams.scss'
})
export class Teams implements OnInit {
  private api = inject(TeamApi);
  private toast = inject(Toast);
  private refresh = inject(DataRefreshService);

  teams = signal<Team[]>([]);
  loading = signal(true);
  loadError = signal<string | null>(null);

  showFormDialog = signal(false);
  editingTeam = signal<Team | null>(null);
  openMenuId = signal<number | null>(null);
  deactivateTarget = signal<Team | null>(null);
  deleteTarget = signal<Team | null>(null);

  showMembersDialog = signal(false);
  membersTeamId = signal<number | null>(null);
  membersTeamName = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.api.getAll().subscribe({
      next: (data) => {
        this.teams.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loadError.set(extractErrorMessage(err, 'Could not load teams. Is the backend running?'));
        this.loading.set(false);
      }
    });
  }

  toggleMenu(id: number): void {
    this.openMenuId.set(this.openMenuId() === id ? null : id);
  }

  closeMenu(): void {
    this.openMenuId.set(null);
  }

  openCreateDialog(): void {
    this.editingTeam.set(null);
    this.showFormDialog.set(true);
  }

  openEditDialog(team: Team): void {
    this.closeMenu();
    this.editingTeam.set(team);
    this.showFormDialog.set(true);
  }

  onDialogSaved(): void {
    this.showFormDialog.set(false);
    this.load();
  }

  askDeactivate(team: Team): void {
    this.closeMenu();
    this.deactivateTarget.set(team);
  }

  confirmDeactivate(): void {
    const target = this.deactivateTarget();
    if (!target) return;
    this.api.deactivate(target.id).subscribe({
      next: () => {
        this.toast.success(`${target.name} deactivated.`);
        this.deactivateTarget.set(null);
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(extractErrorMessage(err, 'Could not deactivate team.'));
        this.deactivateTarget.set(null);
      }
    });
  }

  activate(team: Team): void {
    this.closeMenu();
    this.api.activate(team.id).subscribe({
      next: () => {
        this.toast.success(`${team.name} activated.`);
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(extractErrorMessage(err, 'Could not activate team.'));
      }
    });
  }

  askDelete(team: Team): void {
    this.closeMenu();
    this.deleteTarget.set(team);
  }

  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target) return;
    this.api.delete(target.id).subscribe({
      next: () => {
        this.toast.success(`${target.name} deleted.`);
        this.deleteTarget.set(null);
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(extractErrorMessage(err, 'Could not delete team.'));
        this.deleteTarget.set(null);
      }
    });
  }

  openMembersDialog(team: Team): void {
    this.closeMenu();
    this.membersTeamId.set(team.id);
    this.membersTeamName.set(team.name);
    this.showMembersDialog.set(true);
  }

  onMembersSaved(): void {
    this.showMembersDialog.set(false);
    this.load();
    this.refresh.triggerRefreshCollaborators();
  }

  closeMembersDialog(): void {
    this.showMembersDialog.set(false);
    this.membersTeamId.set(null);
    this.membersTeamName.set(null);
  }
}
