export interface Team {
  id: number;
  name: string;
  description: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TeamRequest {
  name: string;
  description: string;
}
