import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { CategoryService } from '../../../core/services/category.service';
import { PizzaService } from '../../../core/services/pizza.service';
import { ToppingService } from '../../../core/services/topping.service';
import { Category } from '../../../core/models/category.model';
import { Pizza, PizzaSize } from '../../../core/models/pizza.model';
import { Topping } from '../../../core/models/topping.model';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import {
  getPizzaMainImage,
  getPizzaSecondaryImages,
} from '../../../core/utils/pizza.util';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

const PIZZA_SIZES: PizzaSize[] = ['SMALL', 'MEDIUM', 'LARGE'];

@Component({
  selector: 'app-pizza-admin-list',
  imports: [ReactiveFormsModule, PaginationComponent],
  templateUrl: './pizza-admin-list.component.html',
  styleUrl: './pizza-admin-list.component.scss',
})
export class PizzaAdminListComponent {
  private readonly fb = inject(FormBuilder);
  private readonly pizzaService = inject(PizzaService);
  private readonly categoryService = inject(CategoryService);
  private readonly toppingService = inject(ToppingService);

  readonly pizzas = signal<Pizza[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly toppings = signal<Topping[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly selectedCategoryId = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly selectedMainImageFile = signal<File | null>(null);
  readonly selectedSecondaryImageFiles = signal<File[]>([]);
  readonly mainImagePreviewUrl = signal<string | null>(null);
  readonly secondaryImagePreviewUrls = signal<string[]>([]);
  readonly existingMainImageUrl = signal<string | null>(null);
  readonly existingSecondaryImageUrls = signal<string[]>([]);

  readonly pageSize = 10;
  readonly sizes = PIZZA_SIZES;

  readonly form = this.fb.nonNullable.group({
    categoryId: ['', Validators.required],
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: [''],
    basePrice: [0, [Validators.required, Validators.min(0.01)]],
    isActive: [true],
    smallPrice: [0, [Validators.required, Validators.min(0.01)]],
    mediumPrice: [0, [Validators.required, Validators.min(0.01)]],
    largePrice: [0, [Validators.required, Validators.min(0.01)]],
    toppingIds: this.fb.nonNullable.control<string[]>([]),
  });

  constructor() {
    this.loadCategories();
    this.loadToppings();
    this.loadPizzas();
  }

  onCategoryFilterChange(categoryId: string): void {
    this.selectedCategoryId.set(categoryId || null);
    this.page.set(0);
    this.loadPizzas();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.loadPizzas();
  }

  openCreate(): void {
    this.editingId.set(null);
    this.clearImageState();
    this.form.reset({
      categoryId: this.categories()[0]?.id ?? '',
      name: '',
      description: '',
      basePrice: 0,
      isActive: true,
      smallPrice: 0,
      mediumPrice: 0,
      largePrice: 0,
      toppingIds: [],
    });
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  openEdit(pizza: Pizza): void {
    this.editingId.set(pizza.id);
    this.clearImageState();
    this.existingMainImageUrl.set(getPizzaMainImage(pizza));
    this.existingSecondaryImageUrls.set(
      getPizzaSecondaryImages(pizza).map((img) => img.imageUrl),
    );

    const variantMap = Object.fromEntries(
      pizza.variants.map((v) => [v.size, v.price]),
    ) as Record<PizzaSize, number>;

    this.form.patchValue({
      categoryId: pizza.category?.id ?? '',
      name: pizza.name,
      description: pizza.description ?? '',
      basePrice: pizza.basePrice,
      isActive: pizza.active,
      smallPrice: variantMap.SMALL ?? pizza.basePrice,
      mediumPrice: variantMap.MEDIUM ?? pizza.basePrice,
      largePrice: variantMap.LARGE ?? pizza.basePrice,
      toppingIds: pizza.toppings.map((t) => t.id),
    });
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  cancelForm(): void {
    this.clearImageState();
    this.showForm.set(false);
    this.editingId.set(null);
  }

  onMainImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.revokeMainPreviewUrl();
    this.selectedMainImageFile.set(file);
    this.mainImagePreviewUrl.set(file ? URL.createObjectURL(file) : null);
  }

  onSecondaryImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const newFiles = Array.from(input.files ?? []);
    input.value = '';

    if (!newFiles.length) {
      return;
    }

    const previewUrls = newFiles.map((file) => URL.createObjectURL(file));
    this.selectedSecondaryImageFiles.set([
      ...this.selectedSecondaryImageFiles(),
      ...newFiles,
    ]);
    this.secondaryImagePreviewUrls.set([
      ...this.secondaryImagePreviewUrls(),
      ...previewUrls,
    ]);
  }

  removeSecondaryImage(index: number): void {
    const files = [...this.selectedSecondaryImageFiles()];
    const urls = [...this.secondaryImagePreviewUrls()];
    const removedUrl = urls[index];
    if (removedUrl?.startsWith('blob:')) {
      URL.revokeObjectURL(removedUrl);
    }
    files.splice(index, 1);
    urls.splice(index, 1);
    this.selectedSecondaryImageFiles.set(files);
    this.secondaryImagePreviewUrls.set(urls);
  }

  toggleTopping(toppingId: string, checked: boolean): void {
    const current = this.form.controls.toppingIds.value;
    if (checked) {
      this.form.controls.toppingIds.setValue([...current, toppingId]);
    } else {
      this.form.controls.toppingIds.setValue(
        current.filter((id) => id !== toppingId),
      );
    }
  }

  isToppingSelected(toppingId: string): boolean {
    return this.form.controls.toppingIds.value.includes(toppingId);
  }

  getMainImage(pizza: Pizza): string | null {
    return getPizzaMainImage(pizza);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const payload = {
      categoryId: value.categoryId,
      name: value.name,
      description: value.description || undefined,
      basePrice: value.basePrice,
      isActive: value.isActive,
      variants: [
        { size: 'SMALL' as PizzaSize, price: value.smallPrice },
        { size: 'MEDIUM' as PizzaSize, price: value.mediumPrice },
        { size: 'LARGE' as PizzaSize, price: value.largePrice },
      ],
      toppingIds: value.toppingIds,
    };

    this.saving.set(true);
    this.errorMessage.set(null);
    const id = this.editingId();
    const imageUpload = {
      mainImage: this.selectedMainImageFile(),
      secondaryImages: this.selectedSecondaryImageFiles(),
    };

    const request$ = id
      ? this.pizzaService.update(id, payload, imageUpload)
      : this.pizzaService.create(payload, imageUpload);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.cancelForm();
        this.loadPizzas();
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Lưu pizza thất bại.'));
      },
    });
  }

  deletePizza(pizza: Pizza): void {
    if (!confirm(`Xóa pizza "${pizza.name}"?`)) {
      return;
    }

    this.pizzaService.delete(pizza.id).subscribe({
      next: () => this.loadPizzas(),
      error: (err) => {
        alert(err?.error?.message ?? 'Xóa pizza thất bại.');
      },
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  }

  private loadCategories(): void {
    this.categoryService.list(false).subscribe({
      next: (categories) => this.categories.set(categories),
    });
  }

  private loadToppings(): void {
    this.toppingService.list(false).subscribe({
      next: (toppings) => this.toppings.set(toppings),
    });
  }

  private loadPizzas(): void {
    this.loading.set(true);
    this.pizzaService
      .list({
        activeOnly: false,
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

  private clearImageState(): void {
    this.revokeMainPreviewUrl();
    this.revokeSecondaryPreviewUrls();
    this.selectedMainImageFile.set(null);
    this.selectedSecondaryImageFiles.set([]);
    this.mainImagePreviewUrl.set(null);
    this.secondaryImagePreviewUrls.set([]);
    this.existingMainImageUrl.set(null);
    this.existingSecondaryImageUrls.set([]);
  }

  private revokeMainPreviewUrl(): void {
    const preview = this.mainImagePreviewUrl();
    if (preview?.startsWith('blob:')) {
      URL.revokeObjectURL(preview);
    }
  }

  private revokeSecondaryPreviewUrls(): void {
    this.secondaryImagePreviewUrls().forEach((url) => URL.revokeObjectURL(url));
  }
}
