import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Velocity, VelocityRequest } from '../models/velocity';

@Injectable({ providedIn: 'root' })
export class VelocityApi {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/velocities`;

  getAll(): Observable<Velocity[]> {
    return this.http.get<Velocity[]>(this.baseUrl);
  }

  getById(id: number): Observable<Velocity> {
    return this.http.get<Velocity>(`${this.baseUrl}/${id}`);
  }

  create(payload: VelocityRequest): Observable<Velocity> {
    return this.http.post<Velocity>(this.baseUrl, payload);
  }

  update(id: number, payload: VelocityRequest): Observable<Velocity> {
    return this.http.put<Velocity>(`${this.baseUrl}/${id}`, payload);
  }

  validate(id: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/validate`, {});
  }

  unvalidate(id: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/unvalidate`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
