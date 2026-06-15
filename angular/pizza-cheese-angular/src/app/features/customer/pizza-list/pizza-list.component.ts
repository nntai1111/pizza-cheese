import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { CategoryService } from '../../../core/services/category.service';
import { PizzaService } from '../../../core/services/pizza.service';
import { Category } from '../../../core/models/category.model';
import {
  getCategoryImageUrl as resolveCategoryImageUrl,
} from '../../../core/utils/category.util';
import { Pizza } from '../../../core/models/pizza.model';
import { getPizzaMainImage } from '../../../core/utils/pizza.util';
import { UserAvatarComponent } from '../../../shared/components';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-pizza-list',
  imports: [UserAvatarComponent, PaginationComponent],
  templateUrl: './pizza-list.component.html',
  styleUrl: './pizza-list.component.scss',
})
export class PizzaListComponent {
  private readonly authService = inject(AuthService);
  private readonly pizzaService = inject(PizzaService);
  private readonly categoryService = inject(CategoryService);
  private readonly router = inject(Router);

  readonly user = this.authService.currentUser;
  readonly pizzas = signal<Pizza[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly selectedCategoryId = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly pageSize = 12;

  constructor() {
    this.loadCategories();
    this.loadPizzas();
  }

  readonly resolveCategoryImageUrl = resolveCategoryImageUrl;

  onCategoryChange(categoryId: string): void {
    this.selectedCategoryId.set(categoryId || null);
    this.page.set(0);
    this.loadPizzas();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.loadPizzas();
  }

  getMainImage(pizza: Pizza): string | null {
    return getPizzaMainImage(pizza);
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
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
