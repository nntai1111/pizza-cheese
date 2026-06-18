import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { ApiResponse } from '../models/auth.model';
import { CreateOrderRequest, Order, OrderStatus } from '../models/order.model';
import { PageResponse } from '../models/page.model';

const CASHIER_ORDER_BASE = `${API_BASE_URL}/cashier/orders`;

@Injectable({ providedIn: 'root' })
export class CashierService {
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
      .get<ApiResponse<PageResponse<Order>>>(CASHIER_ORDER_BASE, { params: query })
      .pipe(map((response) => response.data));
  }

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http
      .post<ApiResponse<Order>>(CASHIER_ORDER_BASE, request)
      .pipe(map((response) => response.data));
  }

  getOrder(orderId: string): Observable<Order> {
    return this.http
      .get<ApiResponse<Order>>(`${CASHIER_ORDER_BASE}/${orderId}`)
      .pipe(map((response) => response.data));
  }

  confirmPayment(orderId: string): Observable<Order> {
    return this.http
      .post<ApiResponse<Order>>(`${CASHIER_ORDER_BASE}/${orderId}/confirm-payment`, {})
      .pipe(map((response) => response.data));
  }

  cancelOrder(orderId: string): Observable<Order> {
    return this.http
      .post<ApiResponse<Order>>(`${CASHIER_ORDER_BASE}/${orderId}/cancel`, {})
      .pipe(map((response) => response.data));
  }
}
