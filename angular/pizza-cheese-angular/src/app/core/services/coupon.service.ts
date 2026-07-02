import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { ApiResponse } from '../models/auth.model';
import {
  AvailableCoupon,
  Coupon,
  CreateCouponRequest,
  UpdateCouponRequest,
  ValidateCouponRequest,
  ValidateCouponResponse,
} from '../models/coupon.model';

@Injectable({ providedIn: 'root' })
export class CouponService {
  private readonly http = inject(HttpClient);

  validate(request: ValidateCouponRequest): Observable<ValidateCouponResponse> {
    return this.http
      .post<ApiResponse<ValidateCouponResponse>>(`${API_BASE_URL}/coupons/validate`, request)
      .pipe(map((response) => response.data));
  }

  listAvailable(orderAmount: number): Observable<AvailableCoupon[]> {
    const params = new HttpParams().set('orderAmount', String(orderAmount));
    return this.http
      .get<ApiResponse<AvailableCoupon[]>>(`${API_BASE_URL}/coupons/available`, { params })
      .pipe(map((response) => response.data));
  }

  list(activeOnly = false): Observable<Coupon[]> {
    const params = new HttpParams().set('activeOnly', String(activeOnly));
    return this.http
      .get<ApiResponse<Coupon[]>>(`${API_BASE_URL}/admin/coupons`, { params })
      .pipe(map((response) => response.data));
  }

  create(request: CreateCouponRequest): Observable<Coupon> {
    return this.http
      .post<ApiResponse<Coupon>>(`${API_BASE_URL}/admin/coupons`, request)
      .pipe(map((response) => response.data));
  }

  update(id: string, request: UpdateCouponRequest): Observable<Coupon> {
    return this.http
      .put<ApiResponse<Coupon>>(`${API_BASE_URL}/admin/coupons/${id}`, request)
      .pipe(map((response) => response.data));
  }

  delete(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<null>>(`${API_BASE_URL}/admin/coupons/${id}`)
      .pipe(map(() => undefined));
  }
}
