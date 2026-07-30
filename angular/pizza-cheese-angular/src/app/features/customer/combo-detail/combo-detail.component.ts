import {
  Component,
  HostListener,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { map } from 'rxjs';

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

  readonly productId = input<string | null>(null);
  readonly modal = input(false);
  readonly closed = output<void>();

  readonly shop = this.shopContext;

  readonly combo = signal<Combo | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly quantity = signal(1);
  readonly orderNotice = signal<string | null>(null);
  readonly addingToCart = signal(false);
  readonly buyingNow = signal(false);
  readonly lightboxOpen = signal(false);

  readonly formatPrice = formatComboPrice;
  readonly formatVnd = formatVnd;
  readonly getImageUrl = getComboImageUrl;
  readonly getSizeLabel = getPizzaSizeLabel;

  private readonly routeId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('id'))),
    { initialValue: this.route.snapshot.paramMap.get('id') },
  );

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
    effect((onCleanup) => {
      const id = this.productId() ?? this.routeId();
      if (!id) {
        this.loading.set(false);
        this.errorMessage.set('Không tìm thấy combo.');
        return;
      }

      this.loading.set(true);
      this.errorMessage.set(null);
      this.combo.set(null);
      this.quantity.set(1);
      this.orderNotice.set(null);
      this.lightboxOpen.set(false);

      const sub = this.comboService.getById(id).subscribe({
        next: (combo) => {
          this.combo.set(combo);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.errorMessage.set('Không thể tải thông tin combo.');
        },
      });

      onCleanup(() => sub.unsubscribe());
    });

    effect((onCleanup) => {
      if (!this.modal()) {
        return;
      }
      const previous = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      onCleanup(() => {
        document.body.style.overflow = previous;
      });
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (document.querySelector('app-cart-item-detail-modal')) {
      return;
    }
    if (this.lightboxOpen()) {
      this.closeLightbox();
      return;
    }
    if (this.modal()) {
      this.close();
    }
  }

  close(): void {
    this.closeLightbox();
    this.closed.emit();
  }

  openLightbox(): void {
    const combo = this.combo();
    if (!combo || !getComboImageUrl(combo)) {
      return;
    }
    this.lightboxOpen.set(true);
  }

  closeLightbox(): void {
    this.lightboxOpen.set(false);
  }

  goBack(): void {
    if (this.modal()) {
      this.close();
      return;
    }
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

    const request = {
      comboId: combo.id,
      quantity: this.quantity(),
    };
    const busySignal = checkoutImmediately ? this.buyingNow : this.addingToCart;
    busySignal.set(true);
    this.orderNotice.set(null);

    const action$ = checkoutImmediately
      ? this.cartService.buyComboNow(request)
      : this.cartService.addOrMergeCombo(request);

    action$.subscribe({
      next: () => {
        busySignal.set(false);
        if (checkoutImmediately) {
          if (this.modal()) {
            this.close();
          }
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
}
