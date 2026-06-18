import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { ApiResponse } from '../models/auth.model';
import { CreateOrderRequest, Order } from '../models/order.model';

const ORDER_BASE = `${API_BASE_URL}/orders`;
const VNPAY_BASE = `${API_BASE_URL}/payments/vnpay`;

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http
      .post<ApiResponse<Order>>(ORDER_BASE, request)
      .pipe(map((response) => response.data));
  }

  getMyOrders(): Observable<Order[]> {
    return this.http
      .get<ApiResponse<Order[]>>(ORDER_BASE)
      .pipe(map((response) => response.data));
  }

  getOrder(orderId: string): Observable<Order> {
    return this.http
      .get<ApiResponse<Order>>(`${ORDER_BASE}/${orderId}`)
      .pipe(map((response) => response.data));
  }

  cancelOrder(orderId: string): Observable<Order> {
    return this.http
      .post<ApiResponse<Order>>(`${ORDER_BASE}/${orderId}/cancel`, {})
      .pipe(map((response) => response.data));
  }

  getPaymentStatus(txnRef: string): Observable<Order> {
    return this.http
      .get<ApiResponse<Order>>(`${VNPAY_BASE}/status`, { params: { txnRef } })
      .pipe(map((response) => response.data));
  }
}
