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

// One-time login returned by the create endpoint only; never present on reads or updates.
export interface InitialCredentials {
  username: string;
  temporaryPassword: string;
}

export type CollaboratorCreated = Collaborator & { initialCredentials: InitialCredentials };

// ADMIN-only view of an existing collaborator's login, from
// GET /api/collaborators/{id}/credentials. Never present on list/detail responses.
export interface CollaboratorCredentials {
  username: string;
  temporaryPassword: string;
}