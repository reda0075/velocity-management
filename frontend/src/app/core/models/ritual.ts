export interface Ritual {
  id: number;
  name: string;
  durationHours: number;
  createdAt: string;
  updatedAt: string;
}

export interface RitualRequest {
  name: string;
  durationHours: number;
}
