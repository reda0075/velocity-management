import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { VelocityReportResponse } from '../models/report';

@Injectable({ providedIn: 'root' })
export class ReportingApi {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/reports`;

  getVelocityReport(teamId: number, year: number, month: number): Observable<VelocityReportResponse> {
    return this.http.get<VelocityReportResponse>(`${this.baseUrl}/velocity`, {
      params: { teamId: String(teamId), year: String(year), month: String(month) }
    });
  }

  // The backend builds the workbook; the frontend only streams the file.
  exportVelocityReportExcel(teamId: number, year: number, month: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/velocity/export/excel`, {
      responseType: 'blob',
      params: { teamId: String(teamId), year: String(year), month: String(month) }
    });
  }

  // The backend builds the document; the frontend only streams the file.
  exportVelocityReportPdf(teamId: number, year: number, month: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/velocity/export/pdf`, {
      responseType: 'blob',
      params: { teamId: String(teamId), year: String(year), month: String(month) }
    });
  }
}
