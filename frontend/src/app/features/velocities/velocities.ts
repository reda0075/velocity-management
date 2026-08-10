import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { VelocityApi } from '../../core/services/velocity-api';
import { Velocity } from '../../core/models/velocity';
import { CollaboratorApi } from '../../core/services/collaborator-api';
import { Collaborator } from '../../core/models/collaborator';
import { Toast } from '../../core/services/toast';
import { extractErrorMessage } from '../../core/utils/http-error';
import { VelocityFormDialog } from './velocity-form-dialog/velocity-form-dialog';
import { VelocityViewDialog } from './velocity-view-dialog/velocity-view-dialog';
import { ConfirmDialog } from '../../shared/dialogs/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-velocities',
  standalone: true,
  imports: [CommonModule, VelocityFormDialog, VelocityViewDialog, ConfirmDialog],
  templateUrl: './velocities.html',
  styleUrl: './velocities.scss'
})
export class Velocities implements OnInit {
  private api = inject(VelocityApi);
  private collaboratorApi = inject(CollaboratorApi);
  private toast = inject(Toast);

  velocities = signal<Velocity[]>([]);
  collaboratorsMap = signal<Map<number, Collaborator>>(new Map());
  loading = signal(true);
  loadError = signal<string | null>(null);

  showFormDialog = signal(false);
  editingVelocity = signal<Velocity | null>(null);
  viewVelocityId = signal<number | null>(null);
  openMenuId = signal<number | null>(null);
  deleteTarget = signal<Velocity | null>(null);

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
        this.velocities.set(data);
        this.loading.set(false);
        this.loadCollaborators(data);
      },
      error: (err: HttpErrorResponse) => {
        this.loadError.set(extractErrorMessage(err, 'Could not load velocities. Is the backend running?'));
        this.loading.set(false);
      }
    });
  }

  loadCollaborators(velocities: Velocity[]): void {
    const ids = Array.from(new Set(velocities.map(v => v.collaboratorId)));
    if (ids.length === 0) return;

    let loaded = 0;
    const map = new Map<number, Collaborator>();
    ids.forEach(id => {
      this.collaboratorApi.getById(id).subscribe({
        next: (c) => {
          map.set(id, c);
          loaded++;
          if (loaded === ids.length) {
            this.collaboratorsMap.set(map);
          }
        },
        error: () => {
          loaded++;
          if (loaded === ids.length) {
            this.collaboratorsMap.set(map);
          }
        }
      });
    });
  }

  getCollaboratorName(collaboratorId: number): string {
    const c = this.collaboratorsMap().get(collaboratorId);
    if (!c) return 'Unknown';
    return `${c.firstName} ${c.lastName}`;
  }

  closeMenu(): void {
    this.openMenuId.set(null);
  }

  toggleMenu(id: number): void {
    this.openMenuId.set(this.openMenuId() === id ? null : id);
  }

  openCreateDialog(): void {
    this.editingVelocity.set(null);
    this.showFormDialog.set(true);
  }

  openEditDialog(v: Velocity): void {
    this.closeMenu();
    this.editingVelocity.set(v);
    this.showFormDialog.set(true);
  }

  openViewDialog(v: Velocity): void {
    this.closeMenu();
    this.viewVelocityId.set(v.id);
  }

  onDialogSaved(): void {
    this.showFormDialog.set(false);
    this.editingVelocity.set(null);
    this.load();
  }

  askDelete(v: Velocity): void {
    this.closeMenu();
    this.deleteTarget.set(v);
  }

  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target) return;
    this.api.delete(target.id).subscribe({
      next: () => {
        this.toast.success('Velocity calculation deleted.');
        this.deleteTarget.set(null);
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(extractErrorMessage(err, 'Could not delete velocity.'));
        this.deleteTarget.set(null);
      }
    });
  }

  monthName(month: number): string {
    const date = new Date(2000, month - 1, 1);
    return date.toLocaleString('default', { month: 'long' });
  }
}
