import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Ritual, RitualRequest } from '../models/ritual';

@Injectable({ providedIn: 'root' })
export class RitualApi {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/rituals`;

  getAll(): Observable<Ritual[]> {
    return this.http.get<Ritual[]>(this.baseUrl);
  }

  getById(id: number): Observable<Ritual> {
    return this.http.get<Ritual>(`${this.baseUrl}/${id}`);
  }

  create(payload: RitualRequest): Observable<Ritual> {
    return this.http.post<Ritual>(this.baseUrl, payload);
  }

  update(id: number, payload: RitualRequest): Observable<Ritual> {
    return this.http.put<Ritual>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
