import { Component, HostListener, computed, OnInit, effect, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { VelocityApi } from '../../core/services/velocity-api';
import { Velocity, VelocityStatus } from '../../core/models/velocity';
import { CollaboratorApi } from '../../core/services/collaborator-api';
import { Collaborator } from '../../core/models/collaborator';
import { TeamVelocityApi } from '../../core/services/team-velocity-api';
import { TeamVelocity } from '../../core/models/team-velocity';
import { Toast } from '../../core/services/toast';
import { DataRefreshService } from '../../core/services/data-refresh';
import { extractErrorMessage } from '../../core/utils/http-error';
import { VelocityFormDialog } from './velocity-form-dialog/velocity-form-dialog';
import { VelocityViewDialog } from './velocity-view-dialog/velocity-view-dialog';
import { TeamVelocityFormDialog } from './team-velocity-form-dialog/team-velocity-form-dialog';
import { TeamVelocityDetailDialog } from './team-velocity-detail-dialog/team-velocity-detail-dialog';
import { ConfirmDialog } from '../../shared/dialogs/confirm-dialog/confirm-dialog';
import { currentMoroccoPeriod } from '../../core/utils/morocco-date';

type Mode = 'collaborator' | 'team';

// Velocities are tracked per month in Morocco: the default period shown on
// this page is the current month in Africa/Casablanca, not the browser locale.
const CURRENT_PERIOD = currentMoroccoPeriod();
const CURRENT_YEAR = CURRENT_PERIOD.year;
const CURRENT_MONTH = CURRENT_PERIOD.month;

@Component({
  selector: 'app-velocities',
  standalone: true,
  imports: [CommonModule, VelocityFormDialog, VelocityViewDialog, TeamVelocityFormDialog, TeamVelocityDetailDialog, ConfirmDialog],
  templateUrl: './velocities.html',
  styleUrl: './velocities.scss'
})
export class Velocities implements OnInit {
  private api = inject(VelocityApi);
  private collaboratorApi = inject(CollaboratorApi);
  private teamVelocityApi = inject(TeamVelocityApi);
  private toast = inject(Toast);
  private refresh = inject(DataRefreshService);

  mode = signal<Mode>('collaborator');

  velocities = signal<Velocity[]>([]);
  collaboratorsMap = signal<Map<number, Collaborator>>(new Map());
  loading = signal(true);
  loadError = signal<string | null>(null);

  searchText = signal('');
  selectedYear = signal<number>(CURRENT_YEAR);
  selectedMonth = signal<number>(CURRENT_MONTH);

  validatingId = signal<number | null>(null);

  showFormDialog = signal(false);
  editingVelocity = signal<Velocity | null>(null);
  viewVelocityId = signal<number | null>(null);
  openMenuId = signal<number | null>(null);
  deleteTarget = signal<Velocity | null>(null);
  validateTarget = signal<Velocity | null>(null);
  unvalidateTarget = signal<Velocity | null>(null);

  teamVelocities = signal<TeamVelocity[]>([]);
  teamLoading = signal(true);
  teamLoadError = signal<string | null>(null);

  showTeamFormDialog = signal(false);
  teamOpenMenuId = signal<number | null>(null);
  teamDeleteTarget = signal<TeamVelocity | null>(null);
  teamViewId = signal<number | null>(null);

  ngOnInit(): void {
    this.load();
    this.loadTeamVelocities();
  }

  constructor() {
    effect(() => {
      if (this.refresh.refreshTeamVelocities() > 0) {
        this.loadTeamVelocities();
      }
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement | null;
    if (!target || !target.closest('.table__actions-cell')) {
      this.closeMenu();
      this.closeTeamMenu();
    }
  }

  setMode(m: Mode): void {
    this.mode.set(m);
    this.closeMenu();
    this.closeTeamMenu();
  }

  onYearChange(value: string | number): void {
    this.selectedYear.set(Number(value));
  }

  onMonthChange(value: string | number): void {
    this.selectedMonth.set(Number(value));
  }

  // ---- Collaborator velocity ----

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
    this.loadTeamVelocities();
  }

  askValidate(v: Velocity): void {
    this.closeMenu();
    this.validateTarget.set(v);
  }

  confirmValidate(): void {
    const target = this.validateTarget();
    if (!target) return;
    this.validatingId.set(target.id);
    this.api.validate(target.id).subscribe({
      next: () => {
        this.toast.success('Velocity calculation validated.');
        this.validateTarget.set(null);
        this.validatingId.set(null);
        this.load();
        this.loadTeamVelocities();
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(extractErrorMessage(err, 'Could not validate velocity.'));
        this.validateTarget.set(null);
        this.validatingId.set(null);
      }
    });
  }

  askUnvalidate(v: Velocity): void {
    this.closeMenu();
    this.unvalidateTarget.set(v);
  }

  confirmUnvalidate(): void {
    const target = this.unvalidateTarget();
    if (!target) return;
    this.api.unvalidate(target.id).subscribe({
      next: () => {
        this.toast.success('Velocity calculation unvalidated.');
        this.unvalidateTarget.set(null);
        this.load();
        this.loadTeamVelocities();
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(extractErrorMessage(err, 'Could not unvalidate velocity.'));
        this.unvalidateTarget.set(null);
      }
    });
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

  // ---- Team velocity ----

  loadTeamVelocities(): void {
    this.teamLoading.set(true);
    this.teamLoadError.set(null);
    this.teamVelocityApi.getAll().subscribe({
      next: (data) => {
        this.teamVelocities.set(data);
        this.teamLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.teamLoadError.set(extractErrorMessage(err, 'Could not load team velocities. Is the backend running?'));
        this.teamLoading.set(false);
      }
    });
  }

  closeTeamMenu(): void {
    this.teamOpenMenuId.set(null);
  }

  toggleTeamMenu(id: number): void {
    this.teamOpenMenuId.set(this.teamOpenMenuId() === id ? null : id);
  }

  openCreateTeamDialog(): void {
    this.showTeamFormDialog.set(true);
  }

  onTeamDialogSaved(): void {
    this.showTeamFormDialog.set(false);
    this.loadTeamVelocities();
  }

  openTeamDetail(teamVelocity: TeamVelocity): void {
    this.closeTeamMenu();
    this.teamViewId.set(teamVelocity.id);
  }

  askDeleteTeam(teamVelocity: TeamVelocity): void {
    this.closeTeamMenu();
    this.teamDeleteTarget.set(teamVelocity);
  }

  confirmDeleteTeam(): void {
    const target = this.teamDeleteTarget();
    if (!target) return;
    this.teamVelocityApi.delete(target.id).subscribe({
      next: () => {
        this.toast.success(`${target.teamName} team velocity deleted.`);
        this.teamDeleteTarget.set(null);
        this.loadTeamVelocities();
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(extractErrorMessage(err, 'Could not delete team velocity.'));
        this.teamDeleteTarget.set(null);
      }
    });
  }

  months = ['January','February','March','April','May','June','July','August','September','October','November','December'];
  readonly years = Array.from({ length: 6 }, (_, i) => CURRENT_YEAR - i);

  readonly filteredVelocities = computed(() => {
    const q = this.searchText().trim().toLowerCase();
    return this.velocities().filter(v => {
      if (v.year !== this.selectedYear()) return false;
      if (v.month !== this.selectedMonth()) return false;
      if (q) {
        const name = this.getCollaboratorName(v.collaboratorId).toLowerCase();
        if (!name.includes(q) && !String(v.year).includes(q) && !this.monthName(v.month).toLowerCase().includes(q)) return false;
      }
      return true;
    });
  });

  readonly filteredTeamVelocities = computed(() => {
    const q = this.searchText().trim().toLowerCase();
    return this.teamVelocities().filter(tv => {
      if (tv.year !== this.selectedYear()) return false;
      if (tv.month !== this.selectedMonth()) return false;
      if (q && !tv.teamName.toLowerCase().includes(q) && !String(tv.year).includes(q) && !this.monthName(tv.month).toLowerCase().includes(q)) return false;
      return true;
    });
  });

  // ---- Shared helpers ----

  monthName(month: number): string {
    const date = new Date(2000, month - 1, 1);
    return date.toLocaleString('default', { month: 'long' });
  }

  statusLabel(status: VelocityStatus): string {
    return status === 'PENDING_VALIDATION' ? 'Pending validation' : 'Validated';
  }
}
