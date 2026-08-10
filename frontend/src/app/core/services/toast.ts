import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: number;
  type: 'success' | 'error';
  text: string;
}

@Injectable({ providedIn: 'root' })
export class Toast {
  private nextId = 0;
  messages = signal<ToastMessage[]>([]);

  success(text: string): void {
    this.push('success', text);
  }

  error(text: string): void {
    this.push('error', text);
  }

  dismiss(id: number): void {
    this.messages.update(list => list.filter(m => m.id !== id));
  }

  private push(type: 'success' | 'error', text: string): void {
    const id = this.nextId++;
    this.messages.update(list => [...list, { id, type, text }]);
    setTimeout(() => this.dismiss(id), 4000);
  }
}