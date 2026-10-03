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

const COLLAB_PROFILE = {
  id: 1, matricule: 'A1', firstName: 'A', lastName: 'B', profile: 'DEV',
  teamId: 1, teamName: 'Alpha', active: true, createdAt: '', updatedAt: ''
};

const velocityFor = (month: number, status: 'PENDING_VALIDATION' | 'VALIDATED' = 'PENDING_VALIDATION') => ({
  id: month, collaboratorId: 1, year: new Date().getFullYear(), month, workingDays: 20, velocity: 120,
  rituals: [], totalRitualTimeHours: 2, ritualTimeDays: 0.25, effectiveWorkingDays: 19.75,
  velocityRatio: 6, status, createdAt: '', updatedAt: ''
});

const now = new Date();
const CURRENT_MONTH_VELOCITY = velocityFor(now.getMonth() + 1);

/** Flush the personal profile + velocity calls made for a collaborator. */
function flushCollaborator(
  httpMock: HttpTestingController,
  profile: Record<string, unknown> = COLLAB_PROFILE,
  velocities: unknown[] = [CURRENT_MONTH_VELOCITY]
): void {
  httpMock.expectOne((r: HttpRequest<unknown>) => r.url.endsWith('/api/collaborators/me')).flush(profile);
  httpMock.expectOne((r: HttpRequest<unknown>) => r.url.endsWith('/api/velocities/me')).flush(velocities);
}

/** Admin-only endpoints a collaborator must never trigger. */
const ADMIN_ONLY = ['/api/collaborators', '/api/teams', '/api/rituals', '/api/velocities',
  '/api/team-velocities', '/api/reports/velocity'];

/**
 * Fails if a collaborator left an admin-only request pending. Any unexpected
 * request stays in the queue and is rejected by verify().
 */
function expectNoAdminRequests(httpMock: HttpTestingController): void {
  const pending = httpMock.match((r) => ADMIN_ONLY.some(p => r.url.endsWith(p)));
  expect(pending.length).toBe(0);
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
    // Collaborators load their own profile and velocity history.
    flushCollaborator(httpMock);
    expectNoAdminRequests(httpMock);

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.kpi-grid')).toBeFalsy();
    expect(compiled.querySelector('.filter-card')).toBeFalsy();
    expect(compiled.querySelector('.personal-card')).toBeTruthy();
    expect(compiled.querySelector('.personal-card')?.textContent).toContain('Collaborator');
    expect(compiled.querySelector('.personal-card')?.textContent).toContain('A1');
    expect(compiled.querySelector('.personal-card')?.textContent).toContain('Pending validation');
  });

  it('renders every required collaborator field from the backend payloads', () => {
    loginAs('COLLABORATOR');
    const fixture = TestBed.createComponent(Dashboard);
    flushCollaborator(httpMock);
    expectNoAdminRequests(httpMock);

    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    // Identity fields come from /api/collaborators/me.
    expect(text).toContain('A B');            // name
    expect(text).toContain('A1');             // matricule
    expect(text).toContain('DEV');            // profile
    expect(text).toContain('Alpha');          // team

    // Velocity fields come from /api/velocities/me, never recomputed in Angular.
    expect(text).toContain('120');            // current-month velocity, not 120.00
    expect(text).toContain('6.00');           // velocity ratio
    expect(text).toContain('2.00 h');         // ritual time
    expect(text).toContain('19.75');          // effective working days
    expect(text).toContain('Pending validation');
  });

  it('renders a validated velocity with the validated badge', () => {
    loginAs('COLLABORATOR');
    const fixture = TestBed.createComponent(Dashboard);
    flushCollaborator(httpMock, COLLAB_PROFILE, [velocityFor(now.getMonth() + 1, 'VALIDATED')]);

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.badge-success')?.textContent).toContain('Validated');
    expect(compiled.querySelector('.badge-warning')).toBeFalsy();
  });

  it('shows loading states before the personal payloads arrive', () => {
    loginAs('COLLABORATOR');
    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Loading profile');
    expect(compiled.textContent).toContain('Loading current month');
    expect(compiled.textContent).toContain('Loading history');

    flushCollaborator(httpMock);
    fixture.detectChanges();
    expect(compiled.textContent).not.toContain('Loading profile');
  });

  it('shows an error state with retry when the velocity API fails', () => {
    loginAs('COLLABORATOR');
    const fixture = TestBed.createComponent(Dashboard);
    httpMock.expectOne((r: HttpRequest<unknown>) => r.url.endsWith('/api/collaborators/me')).flush(COLLAB_PROFILE);
    httpMock.expectOne((r: HttpRequest<unknown>) => r.url.endsWith('/api/velocities/me')).flush(
      { message: 'boom' }, { status: 500, statusText: 'Server Error' }
    );

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('Your velocity data could not be loaded.');
    expect(compiled.textContent).toContain('Your velocity history could not be loaded.');
    expect(compiled.querySelectorAll('.state--error').length).toBe(2);
    // A failed request must not masquerade as an empty history.
    expect(compiled.textContent).not.toContain('No velocity history is available');
  });

  it('shows an empty state when the collaborator has no velocity history', () => {
    loginAs('COLLABORATOR');
    const fixture = TestBed.createComponent(Dashboard);
    flushCollaborator(httpMock, COLLAB_PROFILE, []);

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.personal-card')).toBeTruthy();
    const empties = Array.from(compiled.querySelectorAll('.state--empty')).map(e => e.textContent ?? '');
    expect(empties.some(t => t.includes('No velocity calculation exists'))).toBe(true);
    expect(empties.some(t => t.includes('No velocity history is available'))).toBe(true);
  });

  it('requests only the personal endpoints for a collaborator', () => {
    loginAs('COLLABORATOR');
    TestBed.createComponent(Dashboard);

    // Only the two /me endpoints resolve; an admin-only request would stay
    // pending and fail the afterEach verify().
    flushCollaborator(httpMock);
    expectNoAdminRequests(httpMock);
  });

  it('never links a collaborator to an admin-only route', () => {
    loginAs('COLLABORATOR');
    const fixture = TestBed.createComponent(Dashboard);
    flushCollaborator(httpMock, COLLAB_PROFILE, []);
    expectNoAdminRequests(httpMock);

    fixture.detectChanges();
    const hrefs = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a'))
      .map(a => a.getAttribute('href'));

    expect(hrefs.length).toBeGreaterThan(0);
    for (const href of hrefs) {
      expect(['/velocities', '/collaborators', '/rituals', '/teams', '/reporting']).not.toContain(href);
    }
  });
});