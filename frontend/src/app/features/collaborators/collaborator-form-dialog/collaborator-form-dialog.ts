import { Component, EventEmitter, Input, OnChanges, Output, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Modal } from '../../../shared/ui/modal/modal';
import { CollaboratorApi } from '../../../core/services/collaborator-api';
import { Collaborator, CollaboratorRequest, Profile } from '../../../core/models/collaborator';
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
  private toast = inject(Toast);

  profiles: Profile[] = ['DEV', 'DEVOPS', 'QA'];
  submitting = signal(false);
  backendError = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.maxLength(50)]],
    profile: ['', [Validators.required]]
  });

  get isEditMode(): boolean {
    return this.collaborator !== null;
  }

  ngOnChanges(): void {
    if (this.collaborator) {
      this.form.setValue({
        firstName: this.collaborator.firstName,
        lastName: this.collaborator.lastName,
        profile: this.collaborator.profile
      });
    } else {
      this.form.reset({ firstName: '', lastName: '', profile: '' });
    }
    this.backendError.set(null);
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
      profile: raw.profile as Profile
    };

    const request$ = this.isEditMode
      ? this.api.update(this.collaborator!.id, payload)
      : this.api.create(payload);

    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success(this.isEditMode ? 'Collaborator updated.' : 'Collaborator created.');
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