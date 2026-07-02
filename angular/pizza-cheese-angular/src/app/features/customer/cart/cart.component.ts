import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { CartItem } from '../../../core/models/cart.model';
import { CartService } from '../../../core/services/cart.service';
import { ShopContextService } from '../../../core/services/shop-context.service';
import {
  getCartItemImage,
  getCartItemTitle,
} from '../../../core/utils/cart-display.util';
import { formatVnd, getPizzaSizeLabel } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { enumEquals } from '../../../core/utils/coded-enum.util';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-cart',
  imports: [RouterLink],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss',
})
export class CartComponent {
  private readonly cartService = inject(CartService);
  private readonly shopContext = inject(ShopContextService);
  private readonly router = inject(Router);

  readonly shop = this.shopContext;

  readonly cart = this.cartService.cart;
  readonly loading = this.cartService.loading;
  readonly selectedSubtotal = this.cartService.selectedSubtotal;
  readonly allItemsSelected = this.cartService.allItemsSelected;
  readonly selectedCount = computed(() => this.cartService.selectedItems().length);
  readonly errorMessage = signal<string | null>(null);
  readonly actionItemId = signal<string | null>(null);

  readonly formatPrice = formatVnd;
  readonly getSizeLabel = getPizzaSizeLabel;
  readonly getItemTitle = getCartItemTitle;
  readonly getItemImage = getCartItemImage;
  readonly isComboItem = (item: CartItem) => enumEquals(item.itemType, 'COMBO');
  readonly isPizzaItem = (item: CartItem) => enumEquals(item.itemType, 'PIZZA');

  constructor() {
    this.cartService.loadCart().subscribe({
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tải giỏ hàng.'));
      },
    });
  }

  decreaseQuantity(item: CartItem): void {
    if (item.quantity <= 1) {
      return;
    }
    this.updateQuantity(item, item.quantity - 1);
  }

  increaseQuantity(item: CartItem): void {
    this.updateQuantity(item, item.quantity + 1);
  }

  removeItem(item: CartItem): void {
    this.actionItemId.set(item.id);
    this.errorMessage.set(null);

    this.cartService.removeItem(item.id).subscribe({
      next: () => this.actionItemId.set(null),
      error: (err: HttpErrorResponse) => {
        this.actionItemId.set(null);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể xóa món.'));
      },
    });
  }

  clearCart(): void {
    this.errorMessage.set(null);
    this.cartService.clearCart().subscribe({
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể xóa giỏ hàng.'));
      },
    });
  }

  isSelected(itemId: string): boolean {
    return this.cartService.isCheckoutSelected(itemId);
  }

  toggleItemSelection(itemId: string): void {
    this.cartService.toggleCheckoutItem(itemId);
  }

  toggleSelectAll(): void {
    this.cartService.toggleSelectAllForCheckout();
  }

  goToCheckout(): void {
    if (this.selectedCount() === 0) {
      this.errorMessage.set('Vui lòng chọn ít nhất một món để thanh toán.');
      return;
    }
    void this.router.navigate(this.shopContext.segments('checkout'));
  }

  getToppingSummary(item: CartItem): string {
    if (!item.toppings.length) {
      return '';
    }
    return item.toppings.map((t) => t.toppingName).join(', ');
  }

  getComboLineSummary(item: CartItem): string {
    if (!item.comboLines.length) {
      return '';
    }
    return item.comboLines
      .map((line) => {
        const size = line.pizzaSize ? getPizzaSizeLabel(line.pizzaSize) : '';
        return `${line.quantity}x ${line.pizzaName}${size ? ` (${size})` : ''}`;
      })
      .join(', ');
  }

  private updateQuantity(item: CartItem, quantity: number): void {
    this.actionItemId.set(item.id);
    this.errorMessage.set(null);

    this.cartService.updateItemQuantity(item.id, { quantity }).subscribe({
      next: () => this.actionItemId.set(null),
      error: (err: HttpErrorResponse) => {
        this.actionItemId.set(null);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể cập nhật số lượng.'));
      },
    });
  }
}
