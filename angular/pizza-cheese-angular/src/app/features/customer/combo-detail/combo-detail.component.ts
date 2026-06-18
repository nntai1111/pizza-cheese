import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { ComboService } from '../../../core/services/combo.service';
import { CartService } from '../../../core/services/cart.service';
import { ShopContextService } from '../../../core/services/shop-context.service';
import { Combo } from '../../../core/models/combo.model';
import {
  formatComboPrice,
  getComboDiscountedPrice,
  getComboImageUrl,
} from '../../../core/utils/combo.util';
import { formatVnd, getPizzaSizeLabel } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';

@Component({
  selector: 'app-combo-detail',
  imports: [RouterLink],
  templateUrl: './combo-detail.component.html',
  styleUrl: './combo-detail.component.scss',
})
export class ComboDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly comboService = inject(ComboService);
  private readonly cartService = inject(CartService);
  private readonly shopContext = inject(ShopContextService);

  readonly shop = this.shopContext;

  readonly combo = signal<Combo | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly quantity = signal(1);
  readonly orderNotice = signal<string | null>(null);
  readonly addingToCart = signal(false);
  readonly buyingNow = signal(false);

  readonly formatPrice = formatComboPrice;
  readonly formatVnd = formatVnd;
  readonly getImageUrl = getComboImageUrl;
  readonly getSizeLabel = getPizzaSizeLabel;

  readonly discountedPrice = computed(() => {
    const current = this.combo();
    return current ? getComboDiscountedPrice(current) : null;
  });

  readonly totalPrice = computed(() => {
    const current = this.combo();
    if (!current) {
      return 0;
    }
    const unit = this.discountedPrice() ?? current.price;
    return unit * this.quantity();
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loading.set(false);
      this.errorMessage.set('Không tìm thấy combo.');
      return;
    }
    this.loadCombo(id);
  }

  goBack(): void {
    this.router.navigate(this.shopContext.segments('combos'));
  }

  increaseQuantity(): void {
    this.quantity.update((q) => q + 1);
  }

  decreaseQuantity(): void {
    this.quantity.update((q) => (q > 1 ? q - 1 : 1));
  }

  addToCart(): void {
    this.addItemToCart(false);
  }

  buyNow(): void {
    this.addItemToCart(true);
  }

  private addItemToCart(checkoutImmediately: boolean): void {
    const combo = this.combo();
    if (!combo) {
      return;
    }

    const previousItemIds = new Set(this.cartService.cart()?.items.map((item) => item.id) ?? []);
    const busySignal = checkoutImmediately ? this.buyingNow : this.addingToCart;
    busySignal.set(true);
    this.orderNotice.set(null);

    this.cartService
      .addCombo({
        comboId: combo.id,
        quantity: this.quantity(),
      })
      .subscribe({
        next: (cart) => {
          busySignal.set(false);
          if (checkoutImmediately) {
            this.cartService.setCheckoutForNewItems(previousItemIds, cart);
            void this.router.navigate(this.shopContext.segments('checkout'));
            return;
          }
          this.orderNotice.set('Đã thêm combo vào giỏ hàng!');
        },
        error: (err: HttpErrorResponse) => {
          busySignal.set(false);
          this.orderNotice.set(getHttpErrorMessage(err, 'Không thể thêm vào giỏ hàng.'));
        },
      });
  }

  private loadCombo(id: string): void {
    this.comboService.getById(id).subscribe({
      next: (combo) => {
        this.combo.set(combo);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Không thể tải thông tin combo.');
      },
    });
  }
}
