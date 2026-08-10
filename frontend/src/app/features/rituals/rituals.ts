import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RitualApi } from '../../core/services/ritual-api';
import { Ritual } from '../../core/models/ritual';
import { Toast } from '../../core/services/toast';
import { extractErrorMessage } from '../../core/utils/http-error';
import { RitualFormDialog } from './ritual-form-dialog/ritual-form-dialog';
import { ConfirmDialog } from '../../shared/dialogs/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-rituals',
  standalone: true,
  imports: [CommonModule, RitualFormDialog, ConfirmDialog],
  templateUrl: './rituals.html',
  styleUrl: './rituals.scss'
})
export class Rituals implements OnInit {
  private api = inject(RitualApi);
  private toast = inject(Toast);

  rituals = signal<Ritual[]>([]);
  loading = signal(true);
  loadError = signal<string | null>(null);

  showFormDialog = signal(false);
  editingRitual = signal<Ritual | null>(null);
  openMenuId = signal<number | null>(null);
  deleteTarget = signal<Ritual | null>(null);

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
        this.rituals.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loadError.set(extractErrorMessage(err, 'Could not load rituals. Is the backend running?'));
        this.loading.set(false);
      }
    });
  }

  closeMenu(): void {
    this.openMenuId.set(null);
  }

  toggleMenu(id: number): void {
    this.openMenuId.set(this.openMenuId() === id ? null : id);
  }

  openCreateDialog(): void {
    this.editingRitual.set(null);
    this.showFormDialog.set(true);
  }

  openEditDialog(r: Ritual): void {
    this.closeMenu();
    this.editingRitual.set(r);
    this.showFormDialog.set(true);
  }

  onDialogSaved(): void {
    this.showFormDialog.set(false);
    this.load();
  }

  askDelete(r: Ritual): void {
    this.closeMenu();
    this.deleteTarget.set(r);
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
        const isFkViolation = err.status === 500 &&
          (err.error?.message === 'An unexpected error occurred' || typeof err.error === 'string');
        if (isFkViolation) {
          this.toast.error('This ritual cannot be deleted because it is currently used in a velocity calculation.');
        } else {
          this.toast.error(extractErrorMessage(err, 'Could not delete ritual.'));
        }
        this.deleteTarget.set(null);
      }
    });
  }
}
