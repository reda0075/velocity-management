import { Component, EventEmitter, Input, OnChanges, Output, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Modal } from '../../../shared/ui/modal/modal';
import { RitualApi } from '../../../core/services/ritual-api';
import { Ritual, RitualRequest } from '../../../core/models/ritual';
import { Toast } from '../../../core/services/toast';
import { extractErrorMessage } from '../../../core/utils/http-error';

@Component({
  selector: 'app-ritual-form-dialog',
  standalone: true,
  imports: [Modal, ReactiveFormsModule],
  templateUrl: './ritual-form-dialog.html',
  styleUrl: './ritual-form-dialog.scss'
})
export class RitualFormDialog implements OnChanges {
  @Input() ritual: Ritual | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private api = inject(RitualApi);
  private toast = inject(Toast);

  submitting = signal(false);
  backendError = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(30)]],
    durationHours: [0, [Validators.required, Validators.min(0.01)]]
  });

  get isEditMode(): boolean {
    return this.ritual !== null;
  }

  ngOnChanges(): void {
    if (this.ritual) {
      this.form.setValue({
        name: this.ritual.name,
        durationHours: this.ritual.durationHours
      });
    } else {
      this.form.reset({ name: '', durationHours: 0 });
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
    const payload: RitualRequest = {
      name: raw.name.trim(),
      durationHours: raw.durationHours
    };

    const request$ = this.isEditMode
      ? this.api.update(this.ritual!.id, payload)
      : this.api.create(payload);

    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success(this.isEditMode ? 'Ritual updated.' : 'Ritual created.');
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
