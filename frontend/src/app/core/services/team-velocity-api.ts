import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TeamVelocity, TeamVelocityDetail, CreateTeamVelocityRequest } from '../models/team-velocity';

@Injectable({ providedIn: 'root' })
export class TeamVelocityApi {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/team-velocities`;

  getAll(): Observable<TeamVelocity[]> {
    return this.http.get<TeamVelocity[]>(this.baseUrl);
  }

  getById(id: number): Observable<TeamVelocity> {
    return this.http.get<TeamVelocity>(`${this.baseUrl}/${id}`);
  }

  getDetails(id: number): Observable<TeamVelocityDetail> {
    return this.http.get<TeamVelocityDetail>(`${this.baseUrl}/${id}/details`);
  }

  create(payload: CreateTeamVelocityRequest): Observable<TeamVelocity> {
    return this.http.post<TeamVelocity>(this.baseUrl, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
