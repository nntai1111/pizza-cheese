import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, finalize, map, tap } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { ApiResponse } from '../models/auth.model';
import {
  AddComboToCartRequest,
  AddPizzaToCartRequest,
  Cart,
  UpdateCartItemRequest,
} from '../models/cart.model';

const CART_BASE = `${API_BASE_URL}/cart`;

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);

  readonly cart = signal<Cart | null>(null);
  readonly loading = signal(false);

  readonly itemCount = computed(() => this.cart()?.itemCount ?? 0);

  loadCart(): Observable<Cart> {
    this.loading.set(true);
    return this.http.get<ApiResponse<Cart>>(CART_BASE).pipe(
      map((response) => response.data),
      tap((cart) => this.cart.set(cart)),
      finalize(() => this.loading.set(false)),
    );
  }

  addPizza(request: AddPizzaToCartRequest): Observable<Cart> {
    return this.http
      .post<ApiResponse<Cart>>(`${CART_BASE}/items/pizza`, request)
      .pipe(
        map((response) => response.data),
        tap((cart) => this.cart.set(cart)),
      );
  }

  addCombo(request: AddComboToCartRequest): Observable<Cart> {
    return this.http
      .post<ApiResponse<Cart>>(`${CART_BASE}/items/combo`, request)
      .pipe(
        map((response) => response.data),
        tap((cart) => this.cart.set(cart)),
      );
  }

  updateItemQuantity(itemId: string, request: UpdateCartItemRequest): Observable<Cart> {
    return this.http
      .patch<ApiResponse<Cart>>(`${CART_BASE}/items/${itemId}`, request)
      .pipe(
        map((response) => response.data),
        tap((cart) => this.cart.set(cart)),
      );
  }

  removeItem(itemId: string): Observable<Cart> {
    return this.http
      .delete<ApiResponse<Cart>>(`${CART_BASE}/items/${itemId}`)
      .pipe(
        map((response) => response.data),
        tap((cart) => this.cart.set(cart)),
      );
  }

  clearCart(): Observable<Cart> {
    return this.http.delete<ApiResponse<Cart>>(CART_BASE).pipe(
      map((response) => response.data),
      tap((cart) => this.cart.set(cart)),
    );
  }
}
