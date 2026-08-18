import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DataRefreshService {
  private _refreshCollaborators = signal(0);
  refreshCollaborators = this._refreshCollaborators.asReadonly();
  triggerRefreshCollaborators() {
    this._refreshCollaborators.update(v => v + 1);
  }

  private _refreshTeams = signal(0);
  refreshTeams = this._refreshTeams.asReadonly();
  triggerRefreshTeams() {
    this._refreshTeams.update(v => v + 1);
  }

  private _refreshTeamVelocities = signal(0);
  refreshTeamVelocities = this._refreshTeamVelocities.asReadonly();
  triggerRefreshTeamVelocities() {
    this._refreshTeamVelocities.update(v => v + 1);
  }
}
