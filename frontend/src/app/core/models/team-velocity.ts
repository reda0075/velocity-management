export type TeamVelocityStatus = 'PENDING_VALIDATION' | 'VALIDATED' | 'NOT_CALCULATED';

export interface CollaboratorVelocitySummary {
  collaboratorId: number;
  collaboratorName: string;
  velocityRatio: number | null;
  status: TeamVelocityStatus;
  includedInCalculation: boolean;
}

export interface TeamVelocity {
  id: number;
  teamId: number;
  teamName: string;
  year: number;
  month: number;
  teamVelocityRatio: number;
  totalMembers: number;
  validatedMembers: number;
  unvalidatedMembers: number;
  createdAt: string;
  updatedAt: string;
}

export interface TeamVelocityDetail extends TeamVelocity {
  members: CollaboratorVelocitySummary[];
}

export interface CreateTeamVelocityRequest {
  teamId: number;
  year: number;
  month: number;
}
