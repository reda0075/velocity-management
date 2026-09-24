// DTO shapes for the reporting API (/api/reports/**).
// These mirror the backend response objects — the backend remains the
// source of truth for all calculations (averages, counts, statuses).

export interface CollaboratorReportEntry {
  collaboratorId: number;
  matricule: string;
  firstName: string;
  lastName: string;
  profile: string;
  velocity: number | null;
  ritualTimeHours: number | null;
  ritualTimeDays: number | null;
  effectiveWorkingDays: number | null;
  status: string | null;
}

export interface VelocityReportResponse {
  teamId: number;
  teamName: string;
  year: number;
  month: number;
  teamMembers: number;
  calculatedMembers: number;
  teamVelocity: number;
  averageIndividualVelocity: number;
  pendingValidations: number;
  validatedVelocities: number;
  collaborators: CollaboratorReportEntry[];
}