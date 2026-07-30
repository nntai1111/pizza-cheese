import { Component, HostListener, computed, effect, input, output } from '@angular/core';

import { CartItem } from '../../../core/models/cart.model';
import {
  getCartItemImage,
  getCartItemTitle,
} from '../../../core/utils/cart-display.util';
import { formatVnd, getPizzaSizeLabel } from '../../../core/utils/pizza.util';
import { enumEquals } from '../../../core/utils/coded-enum.util';

@Component({
  selector: 'app-cart-item-detail-modal',
  templateUrl: './cart-item-detail-modal.component.html',
  styleUrl: './cart-item-detail-modal.component.scss',
})
export class CartItemDetailModalComponent {
  readonly item = input.required<CartItem>();
  readonly closed = output<void>();

  readonly formatPrice = formatVnd;
  readonly getSizeLabel = getPizzaSizeLabel;
  readonly getItemTitle = getCartItemTitle;
  readonly getItemImage = getCartItemImage;

  readonly isPizza = computed(() => enumEquals(this.item().itemType, 'PIZZA'));
  readonly isCombo = computed(() => enumEquals(this.item().itemType, 'COMBO'));

  constructor() {
    effect((onCleanup) => {
      const previous = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      onCleanup(() => {
        document.body.style.overflow = previous;
      });
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.close();
  }

  close(): void {
    this.closed.emit();
  }
}
