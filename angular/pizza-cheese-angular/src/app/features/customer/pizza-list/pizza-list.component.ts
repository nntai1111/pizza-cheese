import { Component, inject, signal } from '@angular/core';

import { CategoryService } from '../../../core/services/category.service';
import { PizzaService } from '../../../core/services/pizza.service';
import { Category } from '../../../core/models/category.model';
import {
  getCategoryImageUrl as resolveCategoryImageUrl,
} from '../../../core/utils/category.util';
import { Pizza } from '../../../core/models/pizza.model';
import {
  formatVnd,
  getPizzaMainImage,
  getPizzaMinPrice,
  getPizzaSizeLabel,
  sortPizzaVariants,
} from '../../../core/utils/pizza.util';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { PizzaDetailComponent } from '../pizza-detail/pizza-detail.component';

@Component({
  selector: 'app-pizza-list',
  imports: [PaginationComponent, PizzaDetailComponent],
  templateUrl: './pizza-list.component.html',
  styleUrl: './pizza-list.component.scss',
})
export class PizzaListComponent {
  private readonly pizzaService = inject(PizzaService);
  private readonly categoryService = inject(CategoryService);

  readonly pizzas = signal<Pizza[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly selectedCategoryId = signal<string | null>(null);
  readonly selectedPizzaId = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly pageSize = 12;
  readonly resolveCategoryImageUrl = resolveCategoryImageUrl;
  readonly formatPrice = formatVnd;
  readonly getMainImage = getPizzaMainImage;
  readonly getMinPrice = getPizzaMinPrice;
  readonly sortVariants = sortPizzaVariants;
  readonly getSizeLabel = getPizzaSizeLabel;

  constructor() {
    this.loadCategories();
    this.loadPizzas();
  }

  onCategoryChange(categoryId: string): void {
    this.selectedCategoryId.set(categoryId || null);
    this.page.set(0);
    this.loadPizzas();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.loadPizzas();
  }

  viewDetail(pizzaId: string): void {
    this.selectedPizzaId.set(pizzaId);
  }

  closeDetail(): void {
    this.selectedPizzaId.set(null);
  }

  private loadCategories(): void {
    this.categoryService.list(true).subscribe({
      next: (categories) => this.categories.set(categories),
    });
  }

  private loadPizzas(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.pizzaService
      .list({
        activeOnly: true,
        categoryId: this.selectedCategoryId(),
        page: this.page(),
        size: this.pageSize,
      })
      .subscribe({
        next: (result) => {
          this.pizzas.set(result.content);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('Không thể tải danh sách pizza.');
          this.loading.set(false);
        },
      });
  }
}
