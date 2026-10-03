import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth';
import { CollaboratorApi } from '../../core/services/collaborator-api';
import { TeamApi } from '../../core/services/team-api';
import { VelocityApi } from '../../core/services/velocity-api';
import { ReportingApi } from '../../core/services/reporting-api';
import { TeamVelocityApi } from '../../core/services/team-velocity-api';
import { Collaborator } from '../../core/models/collaborator';
import { Team } from '../../core/models/team';
import { VelocityReportResponse } from '../../core/models/report';
import { Velocity } from '../../core/models/velocity';
import { TeamVelocity } from '../../core/models/team-velocity';
import { extractErrorMessage } from '../../core/utils/http-error';
import { KpiCard } from './kpi-card';

type DashboardState = 'loading' | 'empty' | 'error' | 'ready';

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

const CURRENT_YEAR = new Date().getFullYear();
const CURRENT_MONTH = new Date().getMonth() + 1;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, KpiCard],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard {
  private auth = inject(AuthService);
  private collaboratorApi = inject(CollaboratorApi);
  private teamApi = inject(TeamApi);
  private reportingApi = inject(ReportingApi);
  private velocityApi = inject(VelocityApi);
  private teamVelocityApi = inject(TeamVelocityApi);

  readonly months = MONTHS;
  readonly years = Array.from({ length: 6 }, (_, i) => CURRENT_YEAR - i);

  readonly currentYear = CURRENT_YEAR;
  readonly currentMonth = CURRENT_MONTH;

  // ---- Selection ----

  selectedTeamId = signal<number | null>(null);
  selectedYear = signal<number>(CURRENT_YEAR);
  selectedMonth = signal<number>(CURRENT_MONTH);

  // ---- Overview data (role-aware) ----

  collaborators = signal<Collaborator[]>([]);
  teams = signal<Team[]>([]);
  teamVelocities = signal<TeamVelocity[]>([]);

  overviewLoading = signal(true);
  overviewError = signal<string | null>(null);

  // ---- Selected-team report ----

  report = signal<VelocityReportResponse | null>(null);
  reportState = signal<DashboardState>('loading');

  // ---- Collaborator personal view ----
  profile = signal<Collaborator | null>(null);
  profileState = signal<DashboardState>('loading');
  velocities = signal<Velocity[]>([]);
  velocitiesState = signal<DashboardState>('loading');

  constructor() {
    if (this.isManager()) {
      this.loadOverview();
    } else {
      this.loadCollaboratorData();
    }
  }

  /** True for the manager (ADMIN) view. Collaborators see a personal summary only. */
  readonly isManager = computed(() => this.auth.hasRole('ADMIN'));

  // ---- Computed ----

  totalCollaborators = computed(() => this.collaborators().filter(c => c.active).length);
  totalTeams = computed(() => this.teams().length);
  activeTeams = computed(() => this.teams().filter(t => t.active).length);

  selectedTeam = computed<Team | null>(() => {
    const id = this.selectedTeamId();
    return id == null ? null : (this.teams().find(t => t.id === id) ?? null);
  });

  selectedTeamName = computed(() => this.selectedTeam()?.name ?? '—');

  /** Username exposed to the template (collaborator view). */
  currentUsername = computed(() => this.auth.currentUser()?.username ?? 'Collaborator');

  reportLoading = computed(() => this.reportState() === 'loading');
  reportEmpty = computed(() => this.reportState() === 'empty');
  reportError = computed(() => this.reportState() === 'error');

  /** KPIs derived entirely from the backend report — no business math here. */
  calculatedMembers = computed(() => this.report()?.calculatedMembers ?? 0);
  pendingValidations = computed(() => this.report()?.pendingValidations ?? 0);
  validatedVelocities = computed(() => this.report()?.validatedVelocities ?? 0);
  teamVelocity = computed(() => this.report()?.teamVelocity ?? 0);
  missingCalculations = computed(() => {
    const r = this.report();
    if (!r) return 0;
    return Math.max(0, r.teamMembers - r.calculatedMembers);
  });

  // ---- Collaborator view computed ----

  profileLoading = computed(() => this.profileState() === 'loading');
  profileEmpty = computed(() => this.profileState() === 'empty');
  profileError = computed(() => this.profileState() === 'error');

  velocitiesLoading = computed(() => this.velocitiesState() === 'loading');
  velocitiesReady = computed(() => this.velocitiesState() === 'ready');
  velocitiesError = computed(() => this.velocitiesState() === 'error');

  currentVelocity = computed<Velocity | null>(() => {
    const year = CURRENT_YEAR;
    const month = CURRENT_MONTH;
    return this.velocities().find(v => v.year === year && v.month === month) ?? null;
  });

  history = computed(() => [...this.velocities()].sort((a, b) =>
    b.year - a.year || b.month - a.month
  ));

  // ---- Selection handlers ----

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

  // ---- Data loading ----

  private loadOverview(): void {
    this.overviewLoading.set(true);
    this.overviewError.set(null);

    this.collaboratorApi.getAll().subscribe({
      next: (data) => this.collaborators.set(data),
      error: () => this.collaborators.set([])
    });

    this.teamApi.getAll().subscribe({
      next: (data) => {
        this.teams.set(data);
        // Default selection: first active team, otherwise null.
        const active = data.find(t => t.active);
        this.selectedTeamId.set(active?.id ?? (data.length ? data[0].id : null));
        this.overviewLoading.set(false);
        this.loadReport();
      },
      error: (err: HttpErrorResponse) => {
        this.overviewError.set(extractErrorMessage(err, 'Could not load teams. Is the backend running?'));
        this.overviewLoading.set(false);
      }
    });

    this.teamVelocityApi.getAll().subscribe({
      next: (data: TeamVelocity[]) => this.teamVelocities.set(data),
      error: () => this.teamVelocities.set([])
    });
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

  // ---- Helpers ----

  private loadCollaboratorData(): void {
    this.profileState.set('loading');
    this.velocitiesState.set('loading');

    this.collaboratorApi.getMine().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.profileState.set('ready');
      },
      error: () => {
        this.profile.set(null);
        this.profileState.set('error');
      }
    });

    this.velocityApi.getMine().subscribe({
      next: (velocities) => {
        this.velocities.set(velocities);
        this.velocitiesState.set(velocities.length ? 'ready' : 'empty');
      },
      error: () => {
        this.velocities.set([]);
        this.velocitiesState.set('error');
      }
    });
  }

  retryCollaborator(): void {
    this.loadCollaboratorData();
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

  monthName(month: number): string {
    return MONTHS[month - 1] ?? String(month);
  }

  periodLabel(v: Velocity): string {
    return `${this.monthName(v.month)} ${v.year}`;
  }

  collaboratorDisplayName(c: { firstName: string; lastName: string }): string {
    return `${c.firstName} ${c.lastName}`;
  }
}