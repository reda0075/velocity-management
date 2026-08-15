import { Component, EventEmitter, Input, OnChanges, Output, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Modal } from '../../../shared/ui/modal/modal';
import { TeamVelocityApi } from '../../../core/services/team-velocity-api';
import { TeamApi } from '../../../core/services/team-api';
import { Team } from '../../../core/models/team';
import { Toast } from '../../../core/services/toast';
import { extractErrorMessage } from '../../../core/utils/http-error';

@Component({
  selector: 'app-team-velocity-form-dialog',
  standalone: true,
  imports: [Modal, ReactiveFormsModule],
  templateUrl: './team-velocity-form-dialog.html',
  styleUrl: './team-velocity-form-dialog.scss'
})
export class TeamVelocityFormDialog implements OnChanges {
  @Input() team: Team | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private api = inject(TeamVelocityApi);
  private teamApi = inject(TeamApi);
  private toast = inject(Toast);

  submitting = signal(false);
  backendError = signal<string | null>(null);

  teams = signal<Team[]>([]);
  loadingTeams = signal(true);

  form = this.fb.nonNullable.group({
    teamId: [null, [Validators.required]],
    year: [new Date().getFullYear(), [Validators.required, Validators.min(2000)]],
    month: [new Date().getMonth() + 1, [Validators.required, Validators.min(1), Validators.max(12)]]
  });

  get isEditMode(): boolean {
    return false;
  }

  ngOnInit(): void {
    this.loadTeams();
  }

  ngOnChanges(): void {
    this.backendError.set(null);
  }

  loadTeams(): void {
    this.loadingTeams.set(true);
    this.teamApi.getAll().subscribe({
      next: (data) => {
        this.teams.set(data.filter(t => t.active));
        this.loadingTeams.set(false);
      },
      error: () => this.loadingTeams.set(false)
    });
  }

  getTeamName(teamId: number): string {
    return this.teams().find(t => t.id === teamId)?.name ?? 'Unknown';
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.backendError.set(null);
    this.submitting.set(true);

    const raw = this.form.getRawValue();
    const payload = {
      teamId: raw.teamId as unknown as number,
      year: raw.year as unknown as number,
      month: raw.month as unknown as number
    };

    this.api.create(payload).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success('Team velocity calculated.');
        this.saved.emit();
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.backendError.set(
          extractErrorMessage(err, 'Something went wrong. Please check the form and try again.')
        );
      }
    });
  }
}
