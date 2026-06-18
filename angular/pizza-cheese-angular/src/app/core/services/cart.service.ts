import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, finalize, map, tap } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { ApiResponse } from '../models/auth.model';
import {
  AddComboToCartRequest,
  AddPizzaToCartRequest,
  Cart,
  CartItem,
  UpdateCartItemRequest,
} from '../models/cart.model';

const CART_BASE = `${API_BASE_URL}/cart`;
const CHECKOUT_SELECTION_KEY = 'pizza_cheese_checkout_selection';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);

  readonly cart = signal<Cart | null>(null);
  readonly loading = signal(false);
  readonly checkoutSelection = signal<Set<string>>(new Set());

  readonly itemCount = computed(() => this.cart()?.itemCount ?? 0);

  readonly selectedItems = computed((): CartItem[] => {
    const cart = this.cart();
    const selection = this.checkoutSelection();
    if (!cart) {
      return [];
    }
    return cart.items.filter((item) => selection.has(item.id));
  });

  readonly selectedSubtotal = computed(() =>
    this.selectedItems().reduce((sum, item) => sum + item.lineTotal, 0),
  );

  readonly allItemsSelected = computed(() => {
    const cart = this.cart();
    if (!cart?.items.length) {
      return false;
    }
    return cart.items.every((item) => this.checkoutSelection().has(item.id));
  });

  constructor() {
    this.restoreCheckoutSelection();
  }

  loadCart(): Observable<Cart> {
    this.loading.set(true);
    return this.http.get<ApiResponse<Cart>>(CART_BASE).pipe(
      map((response) => response.data),
      tap((cart) => {
        this.cart.set(cart);
        this.syncCheckoutSelection(cart);
      }),
      finalize(() => this.loading.set(false)),
    );
  }

  addPizza(request: AddPizzaToCartRequest): Observable<Cart> {
    return this.http
      .post<ApiResponse<Cart>>(`${CART_BASE}/items/pizza`, request)
      .pipe(
        map((response) => response.data),
        tap((cart) => {
          this.cart.set(cart);
          this.syncCheckoutSelection(cart);
        }),
      );
  }

  addCombo(request: AddComboToCartRequest): Observable<Cart> {
    return this.http
      .post<ApiResponse<Cart>>(`${CART_BASE}/items/combo`, request)
      .pipe(
        map((response) => response.data),
        tap((cart) => {
          this.cart.set(cart);
          this.syncCheckoutSelection(cart);
        }),
      );
  }

  updateItemQuantity(itemId: string, request: UpdateCartItemRequest): Observable<Cart> {
    return this.http
      .patch<ApiResponse<Cart>>(`${CART_BASE}/items/${itemId}`, request)
      .pipe(
        map((response) => response.data),
        tap((cart) => {
          this.cart.set(cart);
          this.syncCheckoutSelection(cart);
        }),
      );
  }

  removeItem(itemId: string): Observable<Cart> {
    return this.http
      .delete<ApiResponse<Cart>>(`${CART_BASE}/items/${itemId}`)
      .pipe(
        map((response) => response.data),
        tap((cart) => {
          this.cart.set(cart);
          this.syncCheckoutSelection(cart);
        }),
      );
  }

  clearCart(): Observable<Cart> {
    return this.http.delete<ApiResponse<Cart>>(CART_BASE).pipe(
      map((response) => response.data),
      tap((cart) => {
        this.cart.set(cart);
        this.checkoutSelection.set(new Set());
        this.persistCheckoutSelection();
      }),
    );
  }

  isCheckoutSelected(itemId: string): boolean {
    return this.checkoutSelection().has(itemId);
  }

  toggleCheckoutItem(itemId: string): void {
    this.checkoutSelection.update((current) => {
      const next = new Set(current);
      if (next.has(itemId)) {
        next.delete(itemId);
      } else {
        next.add(itemId);
      }
      return next;
    });
    this.persistCheckoutSelection();
  }

  setCheckoutSelection(itemIds: string[]): void {
    this.checkoutSelection.set(new Set(itemIds));
    this.persistCheckoutSelection();
  }

  selectAllForCheckout(): void {
    const cart = this.cart();
    if (!cart) {
      return;
    }
    this.setCheckoutSelection(cart.items.map((item) => item.id));
  }

  clearCheckoutSelection(): void {
    this.checkoutSelection.set(new Set());
    this.persistCheckoutSelection();
  }

  toggleSelectAllForCheckout(): void {
    if (this.allItemsSelected()) {
      this.clearCheckoutSelection();
      return;
    }
    this.selectAllForCheckout();
  }

  setCheckoutForNewItems(previousItemIds: Set<string>, cart: Cart): void {
    const newItemIds = cart.items
      .filter((item) => !previousItemIds.has(item.id))
      .map((item) => item.id);

    if (newItemIds.length) {
      this.setCheckoutSelection(newItemIds);
      return;
    }

    const lastItem = cart.items.at(-1);
    if (lastItem) {
      this.setCheckoutSelection([lastItem.id]);
    }
  }

  getCheckoutItemIds(): string[] {
    return [...this.checkoutSelection()];
  }

  private syncCheckoutSelection(cart: Cart): void {
    const validIds = new Set(cart.items.map((item) => item.id));
    const next = new Set([...this.checkoutSelection()].filter((id) => validIds.has(id)));

    if (next.size === 0 && cart.items.length > 0) {
      cart.items.forEach((item) => next.add(item.id));
    }

    this.checkoutSelection.set(next);
    this.persistCheckoutSelection();
  }

  private restoreCheckoutSelection(): void {
    const raw = sessionStorage.getItem(CHECKOUT_SELECTION_KEY);
    if (!raw) {
      return;
    }

    try {
      const ids = JSON.parse(raw) as string[];
      if (Array.isArray(ids)) {
        this.checkoutSelection.set(new Set(ids));
      }
    } catch {
      sessionStorage.removeItem(CHECKOUT_SELECTION_KEY);
    }
  }

  private persistCheckoutSelection(): void {
    sessionStorage.setItem(
      CHECKOUT_SELECTION_KEY,
      JSON.stringify(this.getCheckoutItemIds()),
    );
  }
}
