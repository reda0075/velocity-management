import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, ViewChild, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Modal } from '../../../shared/ui/modal/modal';
import { VelocityApi } from '../../../core/services/velocity-api';
import { CollaboratorApi } from '../../../core/services/collaborator-api';
import { RitualApi } from '../../../core/services/ritual-api';
import { Velocity, VelocityRequest } from '../../../core/models/velocity';
import { Collaborator } from '../../../core/models/collaborator';
import { Ritual } from '../../../core/models/ritual';
import { Toast } from '../../../core/services/toast';
import { extractErrorMessage } from '../../../core/utils/http-error';

@Component({
  selector: 'app-velocity-form-dialog',
  standalone: true,
  imports: [Modal, ReactiveFormsModule, CommonModule],
  templateUrl: './velocity-form-dialog.html',
  styleUrl: './velocity-form-dialog.scss'
})
export class VelocityFormDialog implements OnChanges {
  @Input() velocity: Velocity | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  @ViewChild('ritualSelect') ritualSelectRef!: ElementRef<HTMLSelectElement>;

  private fb = inject(FormBuilder);
  private api = inject(VelocityApi);
  private collaboratorApi = inject(CollaboratorApi);
  private ritualApi = inject(RitualApi);
  private toast = inject(Toast);

  submitting = signal(false);
  backendError = signal<string | null>(null);

  collaborators = signal<Collaborator[]>([]);
  rituals = signal<Ritual[]>([]);
  loadingLookups = signal(true);

  form: FormGroup = this.fb.group({
    collaboratorId: [null, [Validators.required]],
    year: [new Date().getFullYear(), [Validators.required, Validators.min(2000)]],
    month: [new Date().getMonth() + 1, [Validators.required, Validators.min(1), Validators.max(12)]],
    workingDays: [null, [Validators.required, Validators.min(1)]],
    velocity: [null, [Validators.required, Validators.min(0.01)]]
  });

  newRitualId = signal<number | null>(null);
  newOccurrences = signal<number | null>(null);

  get isEditMode(): boolean {
    return this.velocity !== null;
  }

  ngOnInit(): void {
    this.loadLookups();
  }

  ngOnChanges(): void {
    if (this.velocity) {
      this.form.patchValue({
        collaboratorId: this.velocity.collaboratorId as unknown as number | null,
        year: this.velocity.year as unknown as number,
        month: this.velocity.month as unknown as number,
        workingDays: this.velocity.workingDays as unknown as number | null,
        velocity: this.velocity.velocity as unknown as number | null
      });
    } else {
      this.form.reset({
        collaboratorId: null,
        year: new Date().getFullYear(),
        month: new Date().getMonth() + 1,
        workingDays: null,
        velocity: null
      });
    }
    this.backendError.set(null);
  }

  loadLookups(): void {
    this.loadingLookups.set(true);
    this.collaboratorApi.getAll().subscribe({
      next: (data) => {
        this.collaborators.set(data.filter(c => c.active));
        this.loadingLookups.set(false);
      },
      error: () => this.loadingLookups.set(false)
    });
    this.ritualApi.getAll().subscribe({
      next: (data) => {
        this.rituals.set(data);
        this.loadingLookups.set(false);
      },
      error: () => this.loadingLookups.set(false)
    });
  }

  addRitual(): void {
    const ritualId = this.newRitualId();
    const occurrences = this.newOccurrences();
    if (!ritualId || !occurrences || occurrences < 1) return;

    if (this.formattedRituals().some(r => r.ritualId === ritualId)) {
      this.toast.error('This ritual is already added.');
      return;
    }

    this.formattedRituals.update(list => [...list, { ritualId, occurrences }]);
    this.newRitualId.set(null);
    this.newOccurrences.set(null);
    this.ritualSelectRef.nativeElement.value = '';
  }

  removeRitual(ritualId: number): void {
    this.formattedRituals.update(list => list.filter(r => r.ritualId !== ritualId));
  }

  getRitualName(ritualId: number): string {
    return this.rituals().find(r => r.id === ritualId)?.name ?? 'Unknown';
  }

  getRitualDuration(ritualId: number): number {
    return this.rituals().find(r => r.id === ritualId)?.durationHours ?? 0;
  }

  formattedRituals = signal<{ ritualId: number; occurrences: number }[]>([]);

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.formattedRituals().length === 0) {
      this.backendError.set('Please add at least one ritual.');
      return;
    }

    this.backendError.set(null);
    this.submitting.set(true);

    const raw = this.form.getRawValue();
    const payload: VelocityRequest = {
      collaboratorId: raw.collaboratorId as unknown as number,
      year: raw.year as unknown as number,
      month: raw.month as unknown as number,
      workingDays: raw.workingDays as unknown as number,
      velocity: raw.velocity as unknown as number,
      rituals: this.formattedRituals().map(r => ({
        ritualId: r.ritualId,
        occurrences: r.occurrences
      }))
    };

    const request$ = this.isEditMode
      ? this.api.update(this.velocity!.id, payload)
      : this.api.create(payload);

    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success(this.isEditMode ? 'Velocity updated.' : 'Velocity calculated.');
        this.saved.emit();
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        if (err.status === 500) {
          this.backendError.set('A velocity calculation for this collaborator and month already exists.');
        } else {
          this.backendError.set(
            extractErrorMessage(err, 'Something went wrong. Please check the form and try again.')
          );
        }
      }
    });
  }
}
