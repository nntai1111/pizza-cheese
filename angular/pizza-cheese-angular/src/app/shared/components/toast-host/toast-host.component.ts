import { Component, inject } from '@angular/core';

import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast-host',
  template: `
    <div class="toast-host" aria-live="polite">
      @for (toast of toastService.messages(); track toast.id) {
        <div class="toast" [class]="'toast toast--' + toast.tone" role="status">
          <span>{{ toast.text }}</span>
          <button type="button" class="toast__close" (click)="toastService.dismiss(toast.id)">
            ×
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .toast-host {
      position: fixed;
      top: 1rem;
      right: 1rem;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      max-width: min(360px, calc(100vw - 2rem));
      pointer-events: none;
    }

    .toast {
      pointer-events: auto;
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.85rem 1rem;
      border-radius: 10px;
      box-shadow: 0 8px 24px rgba(15, 23, 42, 0.18);
      font-size: 0.875rem;
      font-weight: 600;
      line-height: 1.4;
      animation: toast-in 0.18s ease-out;
    }

    .toast--error {
      background: #fef2f2;
      color: #b91c1c;
      border: 1px solid #fecaca;
    }

    .toast--success {
      background: #ecfdf5;
      color: #047857;
      border: 1px solid #a7f3d0;
    }

    .toast--info {
      background: #eff6ff;
      color: #1d4ed8;
      border: 1px solid #bfdbfe;
    }

    .toast__close {
      border: none;
      background: transparent;
      color: inherit;
      cursor: pointer;
      font-size: 1.1rem;
      line-height: 1;
      padding: 0;
      opacity: 0.7;

      &:hover {
        opacity: 1;
      }
    }

    @keyframes toast-in {
      from {
        opacity: 0;
        transform: translateY(-6px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
  `,
})
export class ToastHostComponent {
  readonly toastService = inject(ToastService);
}
