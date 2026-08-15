export type Profile = 'DEV' | 'DEVOPS' | 'QA';

export interface Collaborator {
  id: number;
  matricule: string;
  firstName: string;
  lastName: string;
  profile: Profile;
  teamId: number | null;
  teamName: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

// Payload for create/update — matricule is backend-generated, never sent by the client
export interface CollaboratorRequest {
  firstName: string;
  lastName: string;
  profile: Profile;
  teamId?: number | null;
}