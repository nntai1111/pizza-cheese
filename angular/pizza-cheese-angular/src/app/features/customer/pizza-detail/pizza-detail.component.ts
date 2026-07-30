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

import { PizzaService } from '../../../core/services/pizza.service';
import { CartService } from '../../../core/services/cart.service';
import { ShopContextService } from '../../../core/services/shop-context.service';
import { Pizza, PizzaImage, PizzaVariant } from '../../../core/models/pizza.model';
import {
  formatVnd,
  getPizzaSizeLabel,
  getPizzaSortedImages,
  sortPizzaVariants,
} from '../../../core/utils/pizza.util';
import { codedEnumSame } from '../../../core/utils/coded-enum.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';

@Component({
  selector: 'app-pizza-detail',
  imports: [RouterLink],
  templateUrl: './pizza-detail.component.html',
  styleUrl: './pizza-detail.component.scss',
})
export class PizzaDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly pizzaService = inject(PizzaService);
  private readonly cartService = inject(CartService);
  private readonly shopContext = inject(ShopContextService);

  /** When set (e.g. from list popup), load this id instead of the route param. */
  readonly productId = input<string | null>(null);
  /** Render as overlay popup instead of a full page. */
  readonly modal = input(false);
  readonly closed = output<void>();

  readonly shop = this.shopContext;

  readonly pizza = signal<Pizza | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly activeImageIndex = signal(0);
  readonly selectedVariant = signal<PizzaVariant | null>(null);
  readonly selectedToppingIds = signal<Set<string>>(new Set());
  readonly quantity = signal(1);
  readonly orderNotice = signal<string | null>(null);
  readonly addingToCart = signal(false);
  readonly buyingNow = signal(false);
  readonly lightboxOpen = signal(false);

  readonly formatPrice = formatVnd;
  readonly getSizeLabel = getPizzaSizeLabel;
  readonly sortVariants = sortPizzaVariants;
  readonly isSameSize = codedEnumSame;

  private readonly routeId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('id'))),
    { initialValue: this.route.snapshot.paramMap.get('id') },
  );

  readonly sortedImages = computed(() => {
    const current = this.pizza();
    return current ? getPizzaSortedImages(current) : [];
  });

  readonly activeImage = computed((): PizzaImage | null => {
    const images = this.sortedImages();
    if (!images.length) {
      return null;
    }
    const index = Math.min(this.activeImageIndex(), images.length - 1);
    return images[index];
  });

  readonly activeToppings = computed(() => {
    const current = this.pizza();
    return current?.toppings.filter((t) => t.active) ?? [];
  });

  readonly unitPrice = computed(() => {
    const variant = this.selectedVariant();
    const pizza = this.pizza();
    if (!variant || !pizza) {
      return 0;
    }

    const toppingTotal = pizza.toppings
      .filter((t) => this.selectedToppingIds().has(t.id))
      .reduce((sum, t) => sum + t.price, 0);

    return variant.price + toppingTotal;
  });

  readonly totalPrice = computed(() => this.unitPrice() * this.quantity());

  constructor() {
    effect((onCleanup) => {
      const id = this.productId() ?? this.routeId();
      if (!id) {
        this.loading.set(false);
        this.errorMessage.set('Không tìm thấy pizza.');
        return;
      }

      this.loading.set(true);
      this.errorMessage.set(null);
      this.pizza.set(null);
      this.selectedToppingIds.set(new Set());
      this.quantity.set(1);
      this.orderNotice.set(null);
      this.lightboxOpen.set(false);

      const sub = this.pizzaService.getById(id).subscribe({
        next: (pizza) => {
          this.pizza.set(pizza);
          const variants = sortPizzaVariants(pizza.variants);
          this.selectedVariant.set(variants[0] ?? null);
          this.activeImageIndex.set(0);
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('Không thể tải thông tin pizza.');
          this.loading.set(false);
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

  @HostListener('document:keydown.arrowleft')
  onArrowLeft(): void {
    if (this.lightboxOpen()) {
      this.prevLightboxImage();
    }
  }

  @HostListener('document:keydown.arrowright')
  onArrowRight(): void {
    if (this.lightboxOpen()) {
      this.nextLightboxImage();
    }
  }

  close(): void {
    this.closeLightbox();
    this.closed.emit();
  }

  openLightbox(index?: number): void {
    if (!this.activeImage() && index == null) {
      return;
    }
    if (index != null) {
      this.activeImageIndex.set(index);
    }
    if (!this.sortedImages().length) {
      return;
    }
    this.lightboxOpen.set(true);
  }

  closeLightbox(): void {
    this.lightboxOpen.set(false);
  }

  prevLightboxImage(): void {
    const total = this.sortedImages().length;
    if (total <= 1) {
      return;
    }
    this.activeImageIndex.update((i) => (i - 1 + total) % total);
  }

  nextLightboxImage(): void {
    const total = this.sortedImages().length;
    if (total <= 1) {
      return;
    }
    this.activeImageIndex.update((i) => (i + 1) % total);
  }

  selectImage(index: number): void {
    this.activeImageIndex.set(index);
  }

  selectVariant(variant: PizzaVariant): void {
    this.selectedVariant.set(variant);
  }

  toggleTopping(toppingId: string): void {
    this.selectedToppingIds.update((current) => {
      const next = new Set(current);
      if (next.has(toppingId)) {
        next.delete(toppingId);
      } else {
        next.add(toppingId);
      }
      return next;
    });
  }

  isToppingSelected(toppingId: string): boolean {
    return this.selectedToppingIds().has(toppingId);
  }

  decreaseQuantity(): void {
    if (this.quantity() > 1) {
      this.quantity.update((q) => q - 1);
    }
  }

  increaseQuantity(): void {
    this.quantity.update((q) => q + 1);
  }

  addToCart(): void {
    this.addItemToCart(false);
  }

  buyNow(): void {
    this.addItemToCart(true);
  }

  private addItemToCart(checkoutImmediately: boolean): void {
    const pizza = this.pizza();
    const variant = this.selectedVariant();
    if (!pizza || !variant?.id) {
      this.orderNotice.set('Vui lòng chọn size pizza.');
      return;
    }

    const request = {
      pizzaId: pizza.id,
      pizzaVariantId: variant.id,
      toppingIds: Array.from(this.selectedToppingIds()),
      quantity: this.quantity(),
    };
    const busySignal = checkoutImmediately ? this.buyingNow : this.addingToCart;
    busySignal.set(true);
    this.orderNotice.set(null);

    const action$ = checkoutImmediately
      ? this.cartService.buyPizzaNow(request)
      : this.cartService.addOrMergePizza(request);

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
        this.orderNotice.set('Đã thêm pizza vào giỏ hàng!');
      },
      error: (err: HttpErrorResponse) => {
        busySignal.set(false);
        this.orderNotice.set(getHttpErrorMessage(err, 'Không thể thêm vào giỏ hàng.'));
      },
    });
  }

  goBack(): void {
    if (this.modal()) {
      this.close();
      return;
    }
    this.router.navigate(this.shopContext.segments('pizzas'));
  }
}
