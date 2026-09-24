import { HttpClient, HttpErrorResponse, HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { throwError } from 'rxjs';
import { vi } from 'vitest';
import { authGuard, roleGuard } from '../guards/auth-guard';
import { authInterceptor } from '../interceptors/auth-interceptor';
import { AuthService } from './auth';

const validToken = (expiresAt = Math.floor(Date.now() / 1000) + 3600) =>
  `header.${btoa(JSON.stringify({ exp: expiresAt }))}.signature`;

describe('AuthService and access controls', () => {
  let httpMock: HttpTestingController;
  const router = {
    url: '/teams',
    navigate: vi.fn(),
    createUrlTree: vi.fn((commands: unknown[]) => ({ commands }))
  };

  beforeEach(() => {
    sessionStorage.clear();
    router.navigate.mockReset();
    router.createUrlTree.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: router }
      ]
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function loginAs(role: 'ADMIN' | 'COLLABORATOR'): AuthService {
    const auth = TestBed.inject(AuthService);
    auth.login({ username: role.toLowerCase(), password: 'password' }).subscribe();
    httpMock.expectOne(request => request.url.endsWith('/api/auth/login')).flush({
      id: 1,
      username: role.toLowerCase(),
      role,
      token: validToken()
    });
    return auth;
  }

  it('stores a successful login in session storage and clears it on logout', () => {
    const auth = loginAs('ADMIN');

    expect(auth.isAuthenticated()).toBe(true);
    expect(sessionStorage.getItem('auth_user')).toContain('ADMIN');

    auth.logout();

    expect(auth.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem('auth_user')).toBeNull();
  });

  it('rejects an expired token and clears the session', () => {
    sessionStorage.setItem('auth_user', JSON.stringify({
      id: 2,
      username: 'collaborator',
      role: 'COLLABORATOR',
      token: validToken(Math.floor(Date.now() / 1000) - 1)
    }));
    const auth = TestBed.inject(AuthService);

    expect(auth.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem('auth_user')).toBeNull();
  });

  it('allows the matching role and redirects a mismatched role to the dashboard', () => {
    loginAs('COLLABORATOR');

    const collaboratorResult = TestBed.runInInjectionContext(() =>
      roleGuard('COLLABORATOR')({} as never, {} as never)
    );
    const adminResult = TestBed.runInInjectionContext(() =>
      roleGuard('ADMIN')({} as never, {} as never)
    );

    expect(collaboratorResult).toBe(true);
    expect(adminResult).toEqual({ commands: ['/dashboard'] });
  });

  it('redirects an unauthenticated protected route to login', () => {
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/velocities' } as never)
    );

    expect(result).toEqual({ commands: ['/login'] });
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/velocities' }
    });
  });

  it('logs out and redirects after a 401 response', () => {
    const auth = loginAs('ADMIN');
    const request = new HttpRequest('GET', '/api/teams');

    TestBed.runInInjectionContext(() =>
      authInterceptor(request, () => throwError(() => new HttpErrorResponse({ status: 401 })))
    ).subscribe({ error: () => undefined });

    expect(auth.isAuthenticated()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/teams' }
    });
  });
});
