import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { CategoryService } from '../../../core/services/category.service';
import { Category, CreateCategoryRequest } from '../../../core/models/category.model';
import {
  getCategoryImageUrl as resolveCategoryImageUrl,
  isCategoryActive,
} from '../../../core/utils/category.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';

@Component({
  selector: 'app-category-list',
  imports: [ReactiveFormsModule],
  templateUrl: './category-list.component.html',
  styleUrl: './category-list.component.scss',
})
export class CategoryListComponent {
  private readonly fb = inject(FormBuilder);
  private readonly categoryService = inject(CategoryService);

  readonly categories = signal<Category[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly selectedImageFile = signal<File | null>(null);
  readonly imagePreviewUrl = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    slug: ['', Validators.maxLength(100)],
    description: [''],
    sortOrder: [0],
    isActive: [true],
  });

  constructor() {
    this.loadCategories();
  }

  openCreate(): void {
    this.editingId.set(null);
    this.selectedImageFile.set(null);
    this.imagePreviewUrl.set(null);
    this.form.reset({
      name: '',
      slug: '',
      description: '',
      sortOrder: 0,
      isActive: true,
    });
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  openEdit(category: Category): void {
    this.editingId.set(category.id);
    this.selectedImageFile.set(null);
    this.imagePreviewUrl.set(resolveCategoryImageUrl(category));
    this.form.patchValue({
      name: category.name,
      slug: category.slug,
      description: category.description ?? '',
      sortOrder: category.sortOrder,
      isActive: isCategoryActive(category),
    });
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  cancelForm(): void {
    this.revokePreviewUrl();
    this.showForm.set(false);
    this.editingId.set(null);
    this.selectedImageFile.set(null);
    this.imagePreviewUrl.set(null);
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedImageFile.set(file);

    if (file) {
      this.revokePreviewUrl();
      this.imagePreviewUrl.set(URL.createObjectURL(file));
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);
    const value = this.form.getRawValue();
    const id = this.editingId();
    const payload = this.toRequestPayload(value);
    const imageFile = this.selectedImageFile();

    const request$ = id
      ? this.categoryService.update(id, payload, imageFile)
      : this.categoryService.create(payload, imageFile);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.cancelForm();
        this.loadCategories();
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Lưu danh mục thất bại.'));
      },
    });
  }

  deleteCategory(category: Category): void {
    if (!confirm(`Xóa danh mục "${category.name}"?`)) {
      return;
    }

    this.categoryService.delete(category.id).subscribe({
      next: () => this.loadCategories(),
      error: (err) => {
        alert(err?.error?.message ?? 'Xóa danh mục thất bại.');
      },
    });
  }

  readonly resolveCategoryImageUrl = resolveCategoryImageUrl;
  readonly isCategoryActive = isCategoryActive;

  private toRequestPayload(value: {
    name: string;
    slug: string;
    description: string;
    sortOrder: number;
    isActive: boolean;
  }): CreateCategoryRequest {
    const payload: CreateCategoryRequest = {
      name: value.name.trim(),
      sortOrder: value.sortOrder,
      isActive: value.isActive,
    };

    const slug = value.slug.trim();
    if (slug) {
      payload.slug = slug;
    }

    const description = value.description.trim();
    if (description) {
      payload.description = description;
    }

    return payload;
  }

  private revokePreviewUrl(): void {
    const preview = this.imagePreviewUrl();
    if (preview?.startsWith('blob:')) {
      URL.revokeObjectURL(preview);
    }
  }

  private loadCategories(): void {
    this.loading.set(true);
    this.categoryService.list(false).subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Không thể tải danh sách danh mục.');
        this.loading.set(false);
      },
    });
  }
}
