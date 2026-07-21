import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { ApiResponse, User } from '../models/auth.model';
import { Payment } from '../models/payment.model';
import { PageResponse } from '../models/page.model';
import { PaymentMethod, PaymentStatus, Order } from '../models/order.model';
import { AppRole } from '../enums/role.enum';

const ADMIN_PAYMENTS = `${API_BASE_URL}/admin/payments`;
const ADMIN_STAFF = `${API_BASE_URL}/admin/staff`;
const ADMIN_CUSTOMERS = `${API_BASE_URL}/admin/customers`;

export interface CreateStaffRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
  phone: string;
  role: AppRole;
}

export interface UpdateStaffRequest {
  fullName?: string;
  phone?: string;
  password?: string;
  role?: AppRole;
  active?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  getPayments(params: {
    status?: PaymentStatus;
    method?: PaymentMethod;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  } = {}): Observable<PageResponse<Payment>> {
    const query: Record<string, string> = {
      page: String(params.page ?? 0),
      size: String(params.size ?? 10),
    };
    if (params.status) query['status'] = params.status;
    if (params.method) query['method'] = params.method;
    if (params.from) query['from'] = params.from;
    if (params.to) query['to'] = params.to;

    return this.http
      .get<ApiResponse<PageResponse<Payment>>>(ADMIN_PAYMENTS, { params: query })
      .pipe(map((r) => r.data));
  }

  getPayment(id: string): Observable<Payment> {
    return this.http
      .get<ApiResponse<Payment>>(`${ADMIN_PAYMENTS}/${id}`)
      .pipe(map((r) => r.data));
  }

  getStaff(params: {
    role?: AppRole;
    page?: number;
    size?: number;
  } = {}): Observable<PageResponse<User>> {
    const query: Record<string, string> = {
      page: String(params.page ?? 0),
      size: String(params.size ?? 10),
    };
    if (params.role) query['role'] = params.role;

    return this.http
      .get<ApiResponse<PageResponse<User>>>(ADMIN_STAFF, { params: query })
      .pipe(map((r) => r.data));
  }

  createStaff(request: CreateStaffRequest): Observable<User> {
    return this.http
      .post<ApiResponse<User>>(ADMIN_STAFF, request)
      .pipe(map((r) => r.data));
  }

  updateStaff(id: string, request: UpdateStaffRequest): Observable<User> {
    return this.http
      .put<ApiResponse<User>>(`${ADMIN_STAFF}/${id}`, request)
      .pipe(map((r) => r.data));
  }

  setStaffActive(id: string, active: boolean): Observable<User> {
    return this.http
      .put<ApiResponse<User>>(`${ADMIN_STAFF}/${id}/active`, { active })
      .pipe(map((r) => r.data));
  }

  getCustomers(params: {
    page?: number;
    size?: number;
  } = {}): Observable<PageResponse<User>> {
    const query: Record<string, string> = {
      page: String(params.page ?? 0),
      size: String(params.size ?? 10),
    };
    return this.http
      .get<ApiResponse<PageResponse<User>>>(ADMIN_CUSTOMERS, { params: query })
      .pipe(map((r) => r.data));
  }

  getCustomer(id: string): Observable<User> {
    return this.http
      .get<ApiResponse<User>>(`${ADMIN_CUSTOMERS}/${id}`)
      .pipe(map((r) => r.data));
  }

  getCustomerOrders(
    id: string,
    params: { page?: number; size?: number } = {},
  ): Observable<PageResponse<Order>> {
    const query: Record<string, string> = {
      page: String(params.page ?? 0),
      size: String(params.size ?? 10),
    };
    return this.http
      .get<ApiResponse<PageResponse<Order>>>(`${ADMIN_CUSTOMERS}/${id}/orders`, { params: query })
      .pipe(map((r) => r.data));
  }
}
