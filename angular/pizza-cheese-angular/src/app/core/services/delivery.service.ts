import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { ApiResponse } from '../models/auth.model';
import { Order, OrderStatus } from '../models/order.model';
import { PageResponse } from '../models/page.model';

const DELIVERY_ORDER_BASE = `${API_BASE_URL}/delivery/orders`;

@Injectable({ providedIn: 'root' })
export class DeliveryService {
  private readonly http = inject(HttpClient);

  getOrders(params: {
    status?: OrderStatus;
    page?: number;
    size?: number;
    updatedSince?: string | null;
  } = {}): Observable<PageResponse<Order>> {
    const query: Record<string, string> = {
      page: String(params.page ?? 0),
      size: String(params.size ?? 10),
    };
    if (params.status) {
      query['status'] = params.status;
    }
    if (params.updatedSince) {
      query['updatedSince'] = params.updatedSince;
    }
    return this.http
      .get<ApiResponse<PageResponse<Order>>>(DELIVERY_ORDER_BASE, { params: query })
      .pipe(map((response) => response.data));
  }

  getOrder(orderId: string): Observable<Order> {
    return this.http
      .get<ApiResponse<Order>>(`${DELIVERY_ORDER_BASE}/${orderId}`)
      .pipe(map((response) => response.data));
  }

  startDelivery(orderId: string): Observable<Order> {
    return this.http
      .post<ApiResponse<Order>>(`${DELIVERY_ORDER_BASE}/${orderId}/start-delivery`, {})
      .pipe(map((response) => response.data));
  }

  markDelivered(orderId: string): Observable<Order> {
    return this.http
      .post<ApiResponse<Order>>(`${DELIVERY_ORDER_BASE}/${orderId}/mark-delivered`, {})
      .pipe(map((response) => response.data));
  }
}
