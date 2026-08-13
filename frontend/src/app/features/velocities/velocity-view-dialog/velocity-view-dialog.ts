import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Modal } from '../../../shared/ui/modal/modal';
import { VelocityApi } from '../../../core/services/velocity-api';
import { Velocity } from '../../../core/models/velocity';
import { CollaboratorApi } from '../../../core/services/collaborator-api';
import { Collaborator } from '../../../core/models/collaborator';

@Component({
  selector: 'app-velocity-view-dialog',
  standalone: true,
  imports: [CommonModule, Modal],
  templateUrl: './velocity-view-dialog.html',
  styleUrl: './velocity-view-dialog.scss'
})
export class VelocityViewDialog implements OnInit {
  @Input() velocityId: number | null = null;
  @Output() closed = new EventEmitter<void>();

  private api = inject(VelocityApi);
  private collaboratorApi = inject(CollaboratorApi);

  velocity = signal<Velocity | null>(null);
  collaborator = signal<Collaborator | null>(null);
  loading = signal(true);
  loadError = signal<string | null>(null);

  ngOnInit(): void {
    if (!this.velocityId) return;
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.api.getById(this.velocityId!).subscribe({
      next: (v) => {
        this.velocity.set(v);
        this.collaboratorApi.getById(v.collaboratorId).subscribe({
          next: (c) => this.collaborator.set(c),
          error: () => this.collaborator.set(null)
        });
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Could not load velocity details.');
        this.loading.set(false);
      }
    });
  }

  monthName(month: number): string {
    const date = new Date(2000, month - 1, 1);
    return date.toLocaleString('default', { month: 'long' });
  }
}
