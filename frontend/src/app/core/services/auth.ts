import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export type AuthRole = 'ADMIN' | 'COLLABORATOR';

export interface AuthUser {
  id: number;
  username: string;
  role: AuthRole;
  token: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'auth_user';
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/auth`;

  currentUser = signal<AuthUser | null>(this.readFromStorage());

  login(credentials: LoginRequest): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${this.baseUrl}/login`, credentials)
      .pipe(
        tap(res => this.saveToStorage(res))
      );
  }

  logout(): void {
    sessionStorage.removeItem(this.storageKey);
    this.currentUser.set(null);
  }

  getToken(): string | null {
    return this.isAuthenticated() ? this.currentUser()?.token ?? null : null;
  }

  isAuthenticated(): boolean {
    const user = this.currentUser();
    if (!user || this.isTokenExpired(user.token)) {
      this.logout();
      return false;
    }

    return true;
  }

  hasRole(role: AuthRole): boolean {
    return this.isAuthenticated() && this.currentUser()?.role === role;
  }

  landingPath(): string {
    return '/dashboard';
  }

  private saveToStorage(user: AuthUser): void {
    sessionStorage.setItem(this.storageKey, JSON.stringify(user));
    this.currentUser.set(user);
  }

  private readFromStorage(): AuthUser | null {
    const raw = sessionStorage.getItem(this.storageKey);
    if (!raw) return null;

    try {
      const user = JSON.parse(raw) as AuthUser;
      return user.token && (user.role === 'ADMIN' || user.role === 'COLLABORATOR')
        ? user
        : null;
    } catch {
      sessionStorage.removeItem(this.storageKey);
      return null;
    }
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = token.split('.')[1];
      if (!payload) return true;

      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const decoded = JSON.parse(atob(normalized)) as { exp?: number };
      return !decoded.exp || decoded.exp * 1000 <= Date.now();
    } catch {
      return true;
    }
  }
}
