import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Modal } from '../../ui/modal/modal';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [Modal],
  templateUrl: './confirm-dialog.html',
  styleUrl: './confirm-dialog.scss'
})
export class ConfirmDialog {
  @Input() title = 'Are you sure?';
  @Input() message = '';
  @Input() confirmLabel = 'Confirm';
  @Input() danger = false;
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
}