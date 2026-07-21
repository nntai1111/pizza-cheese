import { Injectable, signal } from '@angular/core';

export type ToastTone = 'error' | 'success' | 'info';

export interface ToastMessage {
  id: number;
  text: string;
  tone: ToastTone;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  readonly messages = signal<ToastMessage[]>([]);

  error(text: string, durationMs = 3500): void {
    this.show(text, 'error', durationMs);
  }

  success(text: string, durationMs = 2500): void {
    this.show(text, 'success', durationMs);
  }

  info(text: string, durationMs = 2500): void {
    this.show(text, 'info', durationMs);
  }

  dismiss(id: number): void {
    this.messages.update((items) => items.filter((item) => item.id !== id));
  }

  private show(text: string, tone: ToastTone, durationMs: number): void {
    const id = this.nextId++;
    this.messages.update((items) => [...items, { id, text, tone }]);
    window.setTimeout(() => this.dismiss(id), durationMs);
  }
}
