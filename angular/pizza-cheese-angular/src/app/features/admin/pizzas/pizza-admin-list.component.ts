import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { CategoryService } from '../../../core/services/category.service';
import { PizzaService } from '../../../core/services/pizza.service';
import { ToppingService } from '../../../core/services/topping.service';
import { ToastService } from '../../../core/services/toast.service';
import { Category } from '../../../core/models/category.model';
import { Pizza, PizzaSize } from '../../../core/models/pizza.model';
import { Topping } from '../../../core/models/topping.model';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { normalizeCodedEnum } from '../../../core/utils/coded-enum.util';
import {
  fieldErrorMessage,
  markFormInvalidAndMessage,
  showFieldError,
} from '../../../core/utils/form-validation.util';
import {
  getPizzaMainImage,
  getPizzaSecondaryImages,
} from '../../../core/utils/pizza.util';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

const PIZZA_SIZES: PizzaSize[] = ['SMALL', 'MEDIUM', 'LARGE'];

const PIZZA_FIELD_LABELS: Record<string, string> = {
  categoryId: 'Danh mục',
  name: 'Tên pizza',
  smallPrice: 'Giá Small',
  mediumPrice: 'Giá Medium',
  largePrice: 'Giá Large',
};

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
  private readonly toast = inject(ToastService);

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
    isActive: [true],
    smallPrice: [null as number | null, [Validators.required, Validators.min(0.01)]],
    mediumPrice: [null as number | null, [Validators.required, Validators.min(0.01)]],
    largePrice: [null as number | null, [Validators.required, Validators.min(0.01)]],
    toppingIds: this.fb.nonNullable.control<string[]>([]),
  });

  constructor() {
    this.loadCategories();
    this.loadToppings();
    this.loadPizzas();
  }

  showError(name: string): boolean {
    return showFieldError(this.form.get(name));
  }

  errorOf(name: string): string | null {
    return fieldErrorMessage(this.form.get(name), this.messagesFor(name));
  }

  controlClass(name: string): string {
    return this.showError(name) ? 'is-invalid' : '';
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
      isActive: true,
      smallPrice: null,
      mediumPrice: null,
      largePrice: null,
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
      pizza.variants.map((v) => [normalizeCodedEnum(v.size), v.price]),
    ) as Record<PizzaSize, number>;

    this.form.patchValue({
      categoryId: pizza.category?.id ?? '',
      name: pizza.name,
      description: pizza.description ?? '',
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

  removeExistingSecondaryImage(index: number): void {
    const urls = [...this.existingSecondaryImageUrls()];
    urls.splice(index, 1);
    this.existingSecondaryImageUrls.set(urls);
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
      this.toast.error(markFormInvalidAndMessage(this.form, PIZZA_FIELD_LABELS));
      return;
    }

    const value = this.form.getRawValue();
    const id = this.editingId();
    const medium = Number(value.mediumPrice);
    const small = Number(value.smallPrice);
    const large = Number(value.largePrice);
    const payload = {
      categoryId: value.categoryId,
      name: value.name,
      description: value.description || undefined,
      basePrice: medium || small || large,
      isActive: value.isActive,
      variants: [
        { size: 'SMALL' as PizzaSize, price: small },
        { size: 'MEDIUM' as PizzaSize, price: medium },
        { size: 'LARGE' as PizzaSize, price: large },
      ],
      toppingIds: value.toppingIds,
      ...(id
        ? { keepSecondaryImageUrls: this.existingSecondaryImageUrls() }
        : {}),
    };

    this.saving.set(true);
    this.errorMessage.set(null);
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
        this.toast.success(id ? 'Đã cập nhật pizza' : 'Đã tạo pizza');
      },
      error: (err) => {
        this.saving.set(false);
        const message = getHttpErrorMessage(err, 'Lưu pizza thất bại.');
        this.errorMessage.set(message);
        this.toast.error(message);
      },
    });
  }

  deletePizza(pizza: Pizza): void {
    if (!confirm(`Xóa pizza "${pizza.name}"?`)) {
      return;
    }

    this.pizzaService.delete(pizza.id).subscribe({
      next: () => {
        this.loadPizzas();
        this.toast.success('Đã xóa pizza');
      },
      error: (err) => {
        this.toast.error(getHttpErrorMessage(err, 'Xóa pizza thất bại.'));
      },
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  }

  private messagesFor(name: string): Partial<Record<string, string>> {
    if (name === 'categoryId') {
      return { required: 'Vui lòng chọn danh mục' };
    }
    if (name === 'name') {
      return { required: 'Vui lòng nhập tên pizza' };
    }
    if (name.endsWith('Price')) {
      return {
        required: 'Vui lòng nhập giá',
        min: 'Giá phải lớn hơn 0',
      };
    }
    return {};
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
    for (const url of this.secondaryImagePreviewUrls()) {
      if (url.startsWith('blob:')) {
        URL.revokeObjectURL(url);
      }
    }
  }

  private loadCategories(): void {
    this.categoryService.list(true).subscribe({
      next: (categories) => this.categories.set(categories),
    });
  }

  private loadToppings(): void {
    this.toppingService.list(true).subscribe({
      next: (toppings) => this.toppings.set(toppings),
    });
  }

  private loadPizzas(): void {
    this.loading.set(true);
    this.pizzaService
      .list({
        activeOnly: false,
        categoryId: this.selectedCategoryId() ?? undefined,
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
