import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Modal } from '../../../shared/ui/modal/modal';
import { TeamVelocityApi } from '../../../core/services/team-velocity-api';
import { TeamVelocityDetail, CollaboratorVelocitySummary } from '../../../core/models/team-velocity';

@Component({
  selector: 'app-team-velocity-detail-dialog',
  standalone: true,
  imports: [CommonModule, Modal],
  templateUrl: './team-velocity-detail-dialog.html',
  styleUrl: './team-velocity-detail-dialog.scss'
})
export class TeamVelocityDetailDialog implements OnInit {
  @Input() teamVelocityId: number | null = null;
  @Output() closed = new EventEmitter<void>();

  private api = inject(TeamVelocityApi);

  detail = signal<TeamVelocityDetail | null>(null);
  loading = signal(true);
  loadError = signal<string | null>(null);

  ngOnInit(): void {
    if (!this.teamVelocityId) return;
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.api.getDetails(this.teamVelocityId!).subscribe({
      next: (d) => {
        this.detail.set(d);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Could not load team velocity details.');
        this.loading.set(false);
      }
    });
  }

  monthName(month: number): string {
    const date = new Date(2000, month - 1, 1);
    return date.toLocaleString('default', { month: 'long' });
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'VALIDATED':
        return 'Validated';
      case 'PENDING_VALIDATION':
        return 'Pending validation';
      case 'NOT_CALCULATED':
        return 'Not calculated';
      default:
        return status;
    }
  }
}
