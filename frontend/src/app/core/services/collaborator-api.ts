import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Collaborator, CollaboratorRequest } from '../models/collaborator';

@Injectable({ providedIn: 'root' })
export class CollaboratorApi {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/collaborators`;

  getAll(): Observable<Collaborator[]> {
    return this.http.get<Collaborator[]>(this.baseUrl);
  }

  getById(id: number): Observable<Collaborator> {
    return this.http.get<Collaborator>(`${this.baseUrl}/${id}`);
  }

  create(payload: CollaboratorRequest): Observable<Collaborator> {
    return this.http.post<Collaborator>(this.baseUrl, payload);
  }

  update(id: number, payload: CollaboratorRequest): Observable<Collaborator> {
    return this.http.put<Collaborator>(`${this.baseUrl}/${id}`, payload);
  }

  activate(id: number): Observable<Collaborator> {
    return this.http.patch<Collaborator>(`${this.baseUrl}/${id}/activate`, {});
  }

  deactivate(id: number): Observable<Collaborator> {
    return this.http.patch<Collaborator>(`${this.baseUrl}/${id}/deactivate`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}