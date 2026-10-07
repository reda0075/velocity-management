import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TeamApi } from '../../core/services/team-api';
import { ReportingApi } from '../../core/services/reporting-api';
import { Team } from '../../core/models/team';
import { VelocityReportResponse } from '../../core/models/report';
import { KpiCard } from '../dashboard/kpi-card';
import { currentMoroccoPeriod } from '../../core/utils/morocco-date';

type ReportState = 'loading' | 'empty' | 'error' | 'ready';

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

// Reports default to the current month in Morocco, matching how
// velocity calculations are tracked.
const CURRENT_PERIOD = currentMoroccoPeriod();
const CURRENT_YEAR = CURRENT_PERIOD.year;
const CURRENT_MONTH = CURRENT_PERIOD.month;

@Component({
  selector: 'app-reporting',
  standalone: true,
  imports: [KpiCard],
  templateUrl: './reporting.html',
  styleUrl: './reporting.scss'
})
export class Reporting {
  private teamApi = inject(TeamApi);
  private reportingApi = inject(ReportingApi);

  readonly months = MONTHS;
  readonly years = Array.from({ length: 6 }, (_, i) => CURRENT_YEAR - i);

  teams = signal<Team[]>([]);
  selectedTeamId = signal<number | null>(null);
  selectedYear = signal<number>(CURRENT_YEAR);
  selectedMonth = signal<number>(CURRENT_MONTH);
  searchText = signal('');

  report = signal<VelocityReportResponse | null>(null);
  reportState = signal<ReportState>('loading');

  readonly reportLoading = computed(() => this.reportState() === 'loading');
  readonly reportEmpty = computed(() => this.reportState() === 'empty');
  readonly reportError = computed(() => this.reportState() === 'error');

  readonly selectedTeam = computed<Team | null>(() => {
    const id = this.selectedTeamId();
    return id == null ? null : (this.teams().find(t => t.id === id) ?? null);
  });

  readonly selectedTeamName = computed(() => this.selectedTeam()?.name ?? '—');

  readonly teamMembers = computed(() => this.report()?.teamMembers ?? 0);
  readonly calculatedMembers = computed(() => this.report()?.calculatedMembers ?? 0);
  readonly teamVelocity = computed(() => this.report()?.teamVelocity ?? 0);
  readonly pendingValidations = computed(() => this.report()?.pendingValidations ?? 0);
  readonly validatedVelocities = computed(() => this.report()?.validatedVelocities ?? 0);
  readonly missingCalculations = computed(() => {
    const r = this.report();
    if (!r) return 0;
    return Math.max(0, r.teamMembers - r.calculatedMembers);
  });

  readonly filteredCollaborators = computed(() => {
    const rows = this.report()?.collaborators ?? [];
    const q = this.searchText().trim().toLowerCase();
    if (!q) return rows;
    return rows.filter(c =>
      `${c.firstName} ${c.lastName}`.toLowerCase().includes(q)
      || c.matricule.toLowerCase().includes(q)
      || c.profile.toLowerCase().includes(q)
    );
  });

  readonly hasCalculations = computed(() => {
    const r = this.report();
    return (r?.calculatedMembers ?? 0) > 0;
  });

  readonly hasFilteredRows = computed(() => this.filteredCollaborators().length > 0);

  constructor() {
    this.teamApi.getAll().subscribe({
      next: (data) => {
        this.teams.set(data);
        const active = data.find(t => t.active);
        this.selectedTeamId.set(active?.id ?? (data.length ? data[0].id : null));
        this.loadReport();
      },
      error: () => this.teams.set([])
    });
  }

  selectTeam(value: string | number | null): void {
    const v = typeof value === 'string' ? Number(value) : value;
    this.selectedTeamId.set(Number.isNaN(v) ? null : v);
    this.loadReport();
  }

  onYearChange(value: string | number): void {
    this.selectedYear.set(Number(value));
    this.loadReport();
  }

  onMonthChange(value: string | number): void {
    this.selectedMonth.set(Number(value));
    this.loadReport();
  }

  onSearchInput(value: string): void {
    this.searchText.set(value);
  }

  clearSearch(): void {
    this.searchText.set('');
  }

  loadReport(): void {
    const teamId = this.selectedTeamId();
    if (teamId == null) {
      this.report.set(null);
      this.reportState.set('empty');
      return;
    }

    this.reportState.set('loading');
    this.reportingApi.getVelocityReport(teamId, this.selectedYear(), this.selectedMonth()).subscribe({
      next: (r) => {
        this.report.set(r);
        this.reportState.set('ready');
      },
      error: (err: HttpErrorResponse) => {
        // A 404 means the team exists but has no velocities for this period — treat as empty.
        if (err.status === 404) {
          this.report.set(null);
          this.reportState.set('empty');
        } else {
          this.report.set(null);
          this.reportState.set('error');
        }
      }
    });
  }

  retryReport(): void {
    this.loadReport();
  }

  monthName(month: number): string {
    return MONTHS[month - 1] ?? String(month);
  }

  collaboratorDisplayName(c: { firstName: string; lastName: string }): string {
    return `${c.firstName} ${c.lastName}`;
  }

  formatRatio(value: number | null | undefined): string {
    return value == null ? '—' : Number(value).toFixed(2);
  }

  formatCount(value: number | null | undefined): string {
    return value == null ? '—' : new Intl.NumberFormat('en-US').format(Number(value));
  }

  statusLabel(status: string | null): string {
    switch (status) {
      case 'VALIDATED': return 'Validated';
      case 'PENDING_VALIDATION': return 'Pending validation';
      default: return 'No calculation';
    }
  }

  statusClass(status: string | null): string {
    switch (status) {
      case 'VALIDATED': return 'badge badge-success';
      case 'PENDING_VALIDATION': return 'badge badge-warning';
      default: return 'badge badge-neutral';
    }
  }

  periodLabel(): string {
    return `${this.monthName(this.selectedMonth())} ${this.selectedYear()}`;
  }

  velocityBarWidth(value: number | null | undefined, max: number): number {
    if (value == null || max <= 0) return 0;
    return Math.min(100, Math.round((Number(value) / max) * 100));
  }

  velocityValue(value: number | null | undefined): number {
    return value == null ? 0 : Number(value);
  }

  readonly velocityMax = computed(() => {
    const values = this.filteredCollaborators();
    const m = Math.max(0, ...values.map(c => this.velocityValue(c.velocity)));
    return m === 0 ? 1 : m;
  });
}
