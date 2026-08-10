import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CollaboratorApi } from '../../core/services/collaborator-api';
import { Collaborator } from '../../core/models/collaborator';
import { Toast } from '../../core/services/toast';
import { extractErrorMessage } from '../../core/utils/http-error';
import { CollaboratorFormDialog } from './collaborator-form-dialog/collaborator-form-dialog';
import { ConfirmDialog } from '../../shared/dialogs/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-collaborators',
  standalone: true,
  imports: [CollaboratorFormDialog, ConfirmDialog],
  templateUrl: './collaborators.html',
  styleUrl: './collaborators.scss'
})
export class Collaborators implements OnInit {
  private api = inject(CollaboratorApi);
  private toast = inject(Toast);

  collaborators = signal<Collaborator[]>([]);
  loading = signal(true);
  loadError = signal<string | null>(null);

  showFormDialog = signal(false);
  editingCollaborator = signal<Collaborator | null>(null);
  openMenuId = signal<number | null>(null);
  deactivateTarget = signal<Collaborator | null>(null);
  deleteTarget = signal<Collaborator | null>(null);

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