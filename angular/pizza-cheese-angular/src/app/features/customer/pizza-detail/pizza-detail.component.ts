import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { PizzaService } from '../../../core/services/pizza.service';
import { Pizza, PizzaImage, PizzaVariant } from '../../../core/models/pizza.model';
import {
  formatVnd,
  getPizzaSizeLabel,
  getPizzaSortedImages,
  sortPizzaVariants,
} from '../../../core/utils/pizza.util';

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

  readonly pizza = signal<Pizza | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly activeImageIndex = signal(0);
  readonly selectedVariant = signal<PizzaVariant | null>(null);
  readonly selectedToppingIds = signal<Set<string>>(new Set());
  readonly quantity = signal(1);
  readonly orderNotice = signal<string | null>(null);

  readonly formatPrice = formatVnd;
  readonly getSizeLabel = getPizzaSizeLabel;
  readonly sortVariants = sortPizzaVariants;

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
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loading.set(false);
      this.errorMessage.set('Không tìm thấy pizza.');
      return;
    }

    this.loadPizza(id);
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
    this.orderNotice.set(
      'Tính năng giỏ hàng đang được phát triển. Bạn đã chọn xong pizza!',
    );
  }

  goBack(): void {
    this.router.navigate(['/customer/pizzas']);
  }

  private loadPizza(id: string): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.pizzaService.getById(id).subscribe({
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
  }
}
