import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { ApiResponse } from '../models/auth.model';
import { Order, OrderStatus } from '../models/order.model';
import { PageResponse } from '../models/page.model';

const KITCHEN_ORDER_BASE = `${API_BASE_URL}/kitchen/orders`;

@Injectable({ providedIn: 'root' })
export class KitchenService {
  private readonly http = inject(HttpClient);

  getOrders(params: {
    status?: OrderStatus;
    page?: number;
    size?: number;
  } = {}): Observable<PageResponse<Order>> {
    const query: Record<string, string> = {
      page: String(params.page ?? 0),
      size: String(params.size ?? 10),
    };
    if (params.status) {
      query['status'] = params.status;
    }
    return this.http
      .get<ApiResponse<PageResponse<Order>>>(KITCHEN_ORDER_BASE, { params: query })
      .pipe(map((response) => response.data));
  }

  getOrder(orderId: string): Observable<Order> {
    return this.http
      .get<ApiResponse<Order>>(`${KITCHEN_ORDER_BASE}/${orderId}`)
      .pipe(map((response) => response.data));
  }

  startPreparing(orderId: string): Observable<Order> {
    return this.http
      .post<ApiResponse<Order>>(`${KITCHEN_ORDER_BASE}/${orderId}/start-preparing`, {})
      .pipe(map((response) => response.data));
  }

  markReady(orderId: string): Observable<Order> {
    return this.http
      .post<ApiResponse<Order>>(`${KITCHEN_ORDER_BASE}/${orderId}/mark-ready`, {})
      .pipe(map((response) => response.data));
  }
}
