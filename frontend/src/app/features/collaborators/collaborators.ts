import { Component, HostListener, computed, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CollaboratorApi } from '../../core/services/collaborator-api';
import { Collaborator, CollaboratorCredentials } from '../../core/models/collaborator';
import { Toast } from '../../core/services/toast';
import { extractErrorMessage } from '../../core/utils/http-error';
import { CollaboratorFormDialog } from './collaborator-form-dialog/collaborator-form-dialog';
import { ConfirmDialog } from '../../shared/dialogs/confirm-dialog/confirm-dialog';
import { Modal } from '../../shared/ui/modal/modal';

@Component({
  selector: 'app-collaborators',
  standalone: true,
  imports: [CollaboratorFormDialog, ConfirmDialog, Modal],
  templateUrl: './collaborators.html',
  styleUrl: './collaborators.scss'
})
export class Collaborators implements OnInit {
  private api = inject(CollaboratorApi);
  private toast = inject(Toast);

  collaborators = signal<Collaborator[]>([]);
  loading = signal(true);
  loadError = signal<string | null>(null);

  searchText = signal('');
  selectedProfile = signal<string>('ALL');
  selectedTeam = signal<string>('ALL');
  selectedStatus = signal<string>('ALL');

  readonly profiles = ['ALL', 'DEV', 'DEVOPS', 'QA'];

  private readonly UNASSIGNED_TEAM = 'Unassigned';

  readonly filteredCollaborators = computed(() => {
    const q = this.searchText().trim().toLowerCase();
    return this.collaborators().filter(c => {
      if (this.selectedProfile() !== 'ALL' && c.profile !== this.selectedProfile()) return false;
      if (this.selectedTeam() !== 'ALL' && (c.teamName ?? this.UNASSIGNED_TEAM) !== this.selectedTeam()) return false;
      if (this.selectedStatus() === 'ACTIVE' && !c.active) return false;
      if (this.selectedStatus() === 'INACTIVE' && c.active) return false;
      if (q) {
        const name = `${c.firstName} ${c.lastName}`.toLowerCase();
        if (!name.includes(q) && !c.matricule.toLowerCase().includes(q)) return false;
      }
      return true;
    });
  });

  readonly teamOptions = computed(() => {
    const teams = new Set<string>();
    this.collaborators().forEach(c => teams.add(c.teamName ?? this.UNASSIGNED_TEAM));
    return Array.from(teams).sort();
  });

  readonly statusOptions = ['ALL', 'ACTIVE', 'INACTIVE'];

  showFormDialog = signal(false);
  editingCollaborator = signal<Collaborator | null>(null);
  openMenuId = signal<number | null>(null);
  deactivateTarget = signal<Collaborator | null>(null);
  deleteTarget = signal<Collaborator | null>(null);

  credentialsTarget = signal<Collaborator | null>(null);
  credentials = signal<CollaboratorCredentials | null>(null);
  credentialsLoading = signal(false);
  credentialsError = signal<string | null>(null);
  copiedCredentialsField = signal<'username' | 'password' | null>(null);

  ngOnInit(): void {
    this.load();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement | null;
    if (!target || !target.closest('.table__actions-cell')) {
      this.closeMenu();
    }
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.api.getAll().subscribe({
      next: (data) => {
        this.collaborators.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loadError.set(extractErrorMessage(err, 'Could not load collaborators. Is the backend running?'));
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
    this.editingCollaborator.set(null);
    this.showFormDialog.set(true);
  }

  openEditDialog(c: Collaborator): void {
    this.closeMenu();
    this.editingCollaborator.set(c);
    this.showFormDialog.set(true);
  }

  onDialogSaved(): void {
    this.showFormDialog.set(false);
    this.load();
  }

  askDeactivate(c: Collaborator): void {
    this.closeMenu();
    this.deactivateTarget.set(c);
  }

  confirmDeactivate(): void {
    const target = this.deactivateTarget();
    if (!target) return;
    this.api.deactivate(target.id).subscribe({
      next: () => {
        this.toast.success(`${target.firstName} ${target.lastName} deactivated.`);
        this.deactivateTarget.set(null);
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(extractErrorMessage(err, 'Could not deactivate collaborator.'));
        this.deactivateTarget.set(null);
      }
    });
  }

  activate(c: Collaborator): void {
    this.closeMenu();
    this.api.activate(c.id).subscribe({
      next: () => {
        this.toast.success(`${c.firstName} ${c.lastName} activated.`);
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(extractErrorMessage(err, 'Could not activate collaborator.'));
      }
    });
  }

  askDelete(c: Collaborator): void {
    this.closeMenu();
    this.deleteTarget.set(c);
  }

  openCredentials(c: Collaborator): void {
    this.closeMenu();
    this.credentialsTarget.set(c);
    this.credentials.set(null);
    this.credentialsError.set(null);
    this.copiedCredentialsField.set(null);
    this.credentialsLoading.set(true);

    this.api.getCredentials(c.id).subscribe({
      next: (credentials) => {
        this.credentials.set(credentials);
        this.credentialsLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.credentials.set(null);
        this.credentialsError.set(extractErrorMessage(
          err,
          'Could not load the credentials for this collaborator.'
        ));
        this.credentialsLoading.set(false);
      }
    });
  }

  closeCredentials(): void {
    this.credentialsTarget.set(null);
    this.credentials.set(null);
    this.credentialsError.set(null);
  }

  copyCredentials(value: string, field: 'username' | 'password'): void {
    navigator.clipboard?.writeText(value).then(
      () => {
        this.copiedCredentialsField.set(field);
        setTimeout(() => this.copiedCredentialsField.set(null), 2000);
      },
      () => this.toast.error('Could not copy to the clipboard.')
    );
  }

  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target) return;
    this.api.delete(target.id).subscribe({
      next: () => {
        this.toast.success(`${target.firstName} ${target.lastName} deleted.`);
        this.deleteTarget.set(null);
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(extractErrorMessage(err, 'Could not delete collaborator.'));
        this.deleteTarget.set(null);
      }
    });
  }
}