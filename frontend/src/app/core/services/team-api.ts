import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Team, TeamRequest } from '../models/team';

@Injectable({ providedIn: 'root' })
export class TeamApi {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/teams`;

  getAll(): Observable<Team[]> {
    return this.http.get<Team[]>(this.baseUrl);
  }

  getById(id: number): Observable<Team> {
    return this.http.get<Team>(`${this.baseUrl}/${id}`);
  }

  create(payload: TeamRequest): Observable<Team> {
    return this.http.post<Team>(this.baseUrl, payload);
  }

  update(id: number, payload: TeamRequest): Observable<Team> {
    return this.http.put<Team>(`${this.baseUrl}/${id}`, payload);
  }

  activate(id: number): Observable<Team> {
    return this.http.patch<Team>(`${this.baseUrl}/${id}/activate`, {});
  }

  deactivate(id: number): Observable<Team> {
    return this.http.patch<Team>(`${this.baseUrl}/${id}/deactivate`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
