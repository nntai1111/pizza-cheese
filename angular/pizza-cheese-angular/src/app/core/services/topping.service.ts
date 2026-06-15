import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { ApiResponse } from '../models/auth.model';
import {
  CreateToppingRequest,
  Topping,
  UpdateToppingRequest,
} from '../models/topping.model';

@Injectable({ providedIn: 'root' })
export class ToppingService {
  private readonly http = inject(HttpClient);

  list(activeOnly = true): Observable<Topping[]> {
    const params = new HttpParams().set('activeOnly', String(activeOnly));
    return this.http
      .get<ApiResponse<Topping[]>>(`${API_BASE_URL}/toppings`, { params })
      .pipe(map((response) => response.data));
  }

  create(request: CreateToppingRequest): Observable<Topping> {
    return this.http
      .post<ApiResponse<Topping>>(`${API_BASE_URL}/admin/toppings`, request)
      .pipe(map((response) => response.data));
  }

  update(id: string, request: UpdateToppingRequest): Observable<Topping> {
    return this.http
      .put<ApiResponse<Topping>>(`${API_BASE_URL}/admin/toppings/${id}`, request)
      .pipe(map((response) => response.data));
  }

  delete(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<null>>(`${API_BASE_URL}/admin/toppings/${id}`)
      .pipe(map(() => undefined));
  }
}
