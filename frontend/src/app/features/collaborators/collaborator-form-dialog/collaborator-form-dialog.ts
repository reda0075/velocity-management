import { Component, EventEmitter, Input, OnChanges, Output, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Modal } from '../../../shared/ui/modal/modal';
import { CollaboratorApi } from '../../../core/services/collaborator-api';
import { TeamApi } from '../../../core/services/team-api';
import { Collaborator, CollaboratorRequest, InitialCredentials, Profile } from '../../../core/models/collaborator';
import { Team } from '../../../core/models/team';
import { Toast } from '../../../core/services/toast';
import { extractErrorMessage } from '../../../core/utils/http-error';

@Component({
  selector: 'app-collaborator-form-dialog',
  standalone: true,
  imports: [Modal, ReactiveFormsModule],
  templateUrl: './collaborator-form-dialog.html',
  styleUrl: './collaborator-form-dialog.scss'
})
export class CollaboratorFormDialog implements OnChanges {
  @Input() collaborator: Collaborator | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private api = inject(CollaboratorApi);
  private teamApi = inject(TeamApi);
  private toast = inject(Toast);

  profiles: Profile[] = ['DEV', 'DEVOPS', 'QA'];
  teams = signal<Team[]>([]);
  submitting = signal(false);
  backendError = signal<string | null>(null);

  /** Shown once after a creation; the backend never returns it again. */
  credentials = signal<(InitialCredentials & { matricule: string }) | null>(null);
  copiedField = signal<'matricule' | 'username' | 'password' | null>(null);

  form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.maxLength(50)]],
    profile: ['', [Validators.required]],
    teamId: [null as number | null]
  });

  get isEditMode(): boolean {
    return this.collaborator !== null;
  }

  get showsCredentials(): boolean {
    return this.credentials() !== null;
  }

  ngOnInit(): void {
    this.loadTeams();
  }

  loadTeams(): void {
    this.teamApi.getAll().subscribe({
      next: (data) => {
        this.teams.set(data.filter(t => t.active));
      },
      error: () => {}
    });
  }

  ngOnChanges(): void {
    if (this.collaborator) {
      this.form.setValue({
        firstName: this.collaborator.firstName,
        lastName: this.collaborator.lastName,
        profile: this.collaborator.profile,
        teamId: this.collaborator.teamId
      });
    } else {
      this.form.reset({ firstName: '', lastName: '', profile: '', teamId: null });
    }
    this.backendError.set(null);
    this.credentials.set(null);
    this.copiedField.set(null);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.backendError.set(null);
    this.submitting.set(true);

    const raw = this.form.getRawValue();
    const payload: CollaboratorRequest = {
      firstName: raw.firstName.trim(),
      lastName: raw.lastName.trim(),
      profile: raw.profile as Profile,
      teamId: raw.teamId as number | null
    };

    const onError = (err: HttpErrorResponse) => {
      this.submitting.set(false);
      this.backendError.set(
        extractErrorMessage(err, 'Something went wrong. Please check the form and try again.')
      );
    };

    if (this.isEditMode) {
      this.api.update(this.collaborator!.id, payload).subscribe({
        next: () => {
          this.submitting.set(false);
          this.toast.success('Collaborator updated.');
          this.saved.emit();
        },
        error: onError
      });
      return;
    }

    // Creation returns the initial credentials, so the dialog stays open to reveal them.
    this.api.create(payload).subscribe({
      next: (created) => {
        this.submitting.set(false);
        this.toast.success('Collaborator created successfully.');
        this.credentials.set({ matricule: created.matricule, ...created.initialCredentials });
      },
      error: onError
    });
  }

  copy(value: string, field: 'matricule' | 'username' | 'password'): void {
    navigator.clipboard?.writeText(value).then(
      () => {
        this.copiedField.set(field);
        setTimeout(() => this.copiedField.set(null), 2000);
      },
      () => this.toast.error('Could not copy to the clipboard.')
    );
  }

  /** Acknowledged by the admin: the list can now reload behind the dialog. */
  done(): void {
    this.saved.emit();
  }
}