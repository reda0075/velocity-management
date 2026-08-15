import { Component, EventEmitter, Input, OnChanges, Output, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Modal } from '../../../shared/ui/modal/modal';
import { TeamApi } from '../../../core/services/team-api';
import { Team } from '../../../core/models/team';
import { Toast } from '../../../core/services/toast';
import { extractErrorMessage } from '../../../core/utils/http-error';

@Component({
  selector: 'app-team-form-dialog',
  standalone: true,
  imports: [Modal, ReactiveFormsModule],
  templateUrl: './team-form-dialog.html',
  styleUrl: './team-form-dialog.scss'
})
export class TeamFormDialog implements OnChanges {
  @Input() team: Team | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private api = inject(TeamApi);
  private toast = inject(Toast);

  submitting = signal(false);
  backendError = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(500)]]
  });

  get isEditMode(): boolean {
    return this.team !== null;
  }

  ngOnChanges(): void {
    if (this.team) {
      this.form.setValue({
        name: this.team.name,
        description: this.team.description || ''
      });
    } else {
      this.form.reset({ name: '', description: '' });
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
    const payload = {
      name: raw.name.trim(),
      description: raw.description?.trim() || ''
    };

    const request$ = this.isEditMode
      ? this.api.update(this.team!.id, payload)
      : this.api.create(payload);

    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success(this.isEditMode ? 'Team updated.' : 'Team created.');
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
