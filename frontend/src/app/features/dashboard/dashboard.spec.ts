import { provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, HttpRequest } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Dashboard } from './dashboard';
import { AuthService } from '../../core/services/auth';

const validToken = (expiresAt = Math.floor(Date.now() / 1000) + 3600) =>
  `header.${btoa(JSON.stringify({ exp: expiresAt }))}.signature`;

function makeUser(role: 'ADMIN' | 'COLLABORATOR') {
  return {
    id: 1,
    username: role === 'ADMIN' ? 'admin' : 'collab',
    role,
    token: validToken()
  };
}

const TEAM = { id: 1, name: 'Alpha', description: '', active: true, createdAt: '', updatedAt: '' };
const ACTIVE_COLLAB = {
  id: 1, matricule: 'A1', firstName: 'A', lastName: 'B', profile: 'DEV',
  teamId: 1, teamName: 'Alpha', active: true, createdAt: '', updatedAt: ''
};
const INACTIVE_COLLAB = {
  id: 2, matricule: 'A2', firstName: 'C', lastName: 'D', profile: 'QA',
  teamId: 1, teamName: 'Alpha', active: false, createdAt: '', updatedAt: ''
};

/** Flush the overview calls that the constructor triggers (collaborators, teams, team-velocities). */
function flushOverview(httpMock: HttpTestingController): void {
  httpMock.expectOne((r: HttpRequest<unknown>) => r.url.endsWith('/api/collaborators')).flush([ACTIVE_COLLAB, INACTIVE_COLLAB]);
  httpMock.expectOne((r: HttpRequest<unknown>) => r.url.endsWith('/api/teams')).flush([TEAM]);
  httpMock.expectOne((r: HttpRequest<unknown>) => r.url.endsWith('/api/team-velocities')).flush([]);
}

describe('Dashboard', () => {
  let httpMock: HttpTestingController;
  let auth: AuthService;

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  function loginAs(role: 'ADMIN' | 'COLLABORATOR'): void {
    auth.login({ username: role.toLowerCase(), password: 'password' }).subscribe();
    httpMock.expectOne((r: HttpRequest<unknown>) => r.url.endsWith('/api/auth/login')).flush(makeUser(role));
  }

  it('renders manager KPIs from the backend report and does not duplicate business math', () => {
    loginAs('ADMIN');
    const fixture = TestBed.createComponent(Dashboard);
    flushOverview(httpMock);
    // The constructor's loadOverview() already triggered the report request.
    httpMock.expectOne((r: HttpRequest<unknown>) => r.url.endsWith('/api/reports/velocity')).flush({
      teamId: 1,
      teamName: 'Alpha',
      year: 2026,
      month: 9,
      teamMembers: 2,
      calculatedMembers: 1,
      teamVelocity: 12.5,
      averageIndividualVelocity: 12.5,
      pendingValidations: 1,
      validatedVelocities: 0,
      collaborators: [
        {
          collaboratorId: 1,
          matricule: 'A1',
          firstName: 'A',
          lastName: 'B',
          profile: 'DEV',
          velocity: 12.5,
          ritualTimeHours: 1.5,
          ritualTimeDays: 0.19,
          effectiveWorkingDays: 7.31,
          status: 'PENDING_VALIDATION'
        }
      ]
    });

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const kpis = Array.from(compiled.querySelectorAll('.kpi-card__value')).map(e => e.textContent);

    expect(kpis).toContain('1'); // active collaborators (1 active of 2)
    expect(kpis).toContain('1'); // teams
    expect(kpis).toContain('1'); // active teams
    expect(kpis).toContain('1'); // calculated members
    expect(kpis).toContain('1'); // pending validations
    expect(kpis).toContain('0'); // validated
    expect(kpis).toContain('12.50'); // team velocity
    expect(kpis).toContain('1'); // missing calculations (2 members - 1 calculated)

    // The pending collaborator is rendered with the pending badge.
    expect(compiled.querySelector('.badge-warning')?.textContent).toContain('Pending');
  });

  it('shows an empty state when the team has no velocities for the period', () => {
    loginAs('ADMIN');
    const fixture = TestBed.createComponent(Dashboard);
    flushOverview(httpMock);
    httpMock.expectOne((r: HttpRequest<unknown>) => r.url.endsWith('/api/reports/velocity')).flush({}, { status: 404, statusText: 'Not Found' });

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.state--empty')).toBeTruthy();
    expect(compiled.querySelector('.state--empty')?.textContent).toContain('No velocity calculations');
  });

  it('shows an error state and a retry button when the report fails', () => {
    loginAs('ADMIN');
    const fixture = TestBed.createComponent(Dashboard);
    flushOverview(httpMock);
    httpMock.expectOne((r: HttpRequest<unknown>) => r.url.endsWith('/api/reports/velocity')).flush(
      { message: 'boom' }, { status: 500, statusText: 'Server Error' }
    );

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.state--error')).toBeTruthy();
    expect(compiled.querySelector('.btn-secondary')?.textContent).toContain('Retry');
  });

  it('does not expose manager metrics to a collaborator', () => {
    loginAs('COLLABORATOR');
    const fixture = TestBed.createComponent(Dashboard);
    flushOverview(httpMock);
    // No report request is made for collaborators — the view is personal only.
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.kpi-grid')).toBeFalsy();
    expect(compiled.querySelector('.filter-card')).toBeFalsy();
    expect(compiled.querySelector('.personal-card')).toBeTruthy();
    expect(compiled.querySelector('.personal-card')?.textContent).toContain('Collaborator');
  });
});