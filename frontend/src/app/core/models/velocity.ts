export interface VelocityRitual {
  ritualId: number;
  ritualName: string;
  durationHours: number;
  occurrences: number;
  totalTimeHours: number;
}

export interface Velocity {
  id: number;
  collaboratorId: number;
  year: number;
  month: number;
  workingDays: number;
  velocity: number;
  rituals: VelocityRitual[];
  totalRitualTimeHours: number;
  ritualTimeDays: number;
  effectiveWorkingDays: number;
  velocityRatio: number;
  createdAt: string;
  updatedAt: string;
}

export interface VelocityRequest {
  collaboratorId: number;
  year: number;
  month: number;
  workingDays: number;
  velocity: number;
  rituals: {
    ritualId: number;
    occurrences: number;
  }[];
}
