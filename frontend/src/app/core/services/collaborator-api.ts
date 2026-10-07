import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Collaborator, CollaboratorCreated, CollaboratorCredentials, CollaboratorRequest } from '../models/collaborator';

@Injectable({ providedIn: 'root' })
export class CollaboratorApi {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/collaborators`;

  getAll(): Observable<Collaborator[]> {
    return this.http.get<Collaborator[]>(this.baseUrl);
  }

  getMine(): Observable<Collaborator> {
    return this.http.get<Collaborator>(`${this.baseUrl}/me`);
  }

  getById(id: number): Observable<Collaborator> {
    return this.http.get<Collaborator>(`${this.baseUrl}/${id}`);
  }

  create(payload: CollaboratorRequest): Observable<CollaboratorCreated> {
    return this.http.post<CollaboratorCreated>(this.baseUrl, payload);
  }

  update(id: number, payload: CollaboratorRequest): Observable<Collaborator> {
    return this.http.put<Collaborator>(`${this.baseUrl}/${id}`, payload);
  }

getCredentials(id: number): Observable<CollaboratorCredentials> {
    return this.http.get<CollaboratorCredentials>(`${this.baseUrl}/${id}/credentials`);
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
