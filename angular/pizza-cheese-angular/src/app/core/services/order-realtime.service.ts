import { Injectable, inject } from '@angular/core';
import { Observable, Subject, finalize } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { AuthService } from './auth.service';

export type OrderRealtimeBoard = 'kitchen' | 'delivery';

export interface OrderRealtimeEvent {
  type: string;
  orderId: string;
  orderCode?: string | null;
  status?: string | null;
  statusCode?: number | null;
  kitchenStaffId?: string | null;
  deliveryStaffId?: string | null;
}

/**
 * Server-Sent Events client for kitchen/delivery boards.
 * Uses access_token query param because EventSource cannot set Authorization.
 */
@Injectable({ providedIn: 'root' })
export class OrderRealtimeService {
  private readonly authService = inject(AuthService);

  connect(board: OrderRealtimeBoard): Observable<OrderRealtimeEvent> {
    const events$ = new Subject<OrderRealtimeEvent>();
    let source: EventSource | null = null;
    let closed = false;
    let retryTimer: ReturnType<typeof setTimeout> | null = null;

    const connect = () => {
      if (closed) {
        return;
      }

      const token = this.authService.getAccessToken();
      if (!token) {
        events$.error(new Error('Missing access token for SSE'));
        return;
      }

      const url = `${API_BASE_URL}/${board}/events?access_token=${encodeURIComponent(token)}`;
      source = new EventSource(url);

      source.addEventListener('order-updated', (message) => {
        try {
          const data = JSON.parse((message as MessageEvent).data) as OrderRealtimeEvent;
          events$.next(data);
        } catch {
          // ignore malformed payloads
        }
      });

      source.onerror = () => {
        source?.close();
        source = null;
        if (!closed) {
          retryTimer = setTimeout(connect, 3000);
        }
      };
    };

    connect();

    return events$.pipe(
      finalize(() => {
        closed = true;
        if (retryTimer) {
          clearTimeout(retryTimer);
        }
        source?.close();
        source = null;
      }),
    );
  }
}
