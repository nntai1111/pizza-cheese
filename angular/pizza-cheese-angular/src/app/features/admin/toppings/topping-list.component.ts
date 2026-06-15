import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { ToppingService } from '../../../core/services/topping.service';
import { Topping } from '../../../core/models/topping.model';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';

@Component({
  selector: 'app-topping-list',
  imports: [ReactiveFormsModule],
  templateUrl: './topping-list.component.html',
  styleUrl: './topping-list.component.scss',
})
export class ToppingListComponent {
  private readonly fb = inject(FormBuilder);
  private readonly toppingService = inject(ToppingService);

  readonly toppings = signal<Topping[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    price: [0, [Validators.required, Validators.min(0)]],
    isActive: [true],
  });

  constructor() {
    this.loadToppings();
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', price: 0, isActive: true });
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  openEdit(topping: Topping): void {
    this.editingId.set(topping.id);
    this.form.patchValue({
      name: topping.name,
      price: topping.price,
      isActive: topping.active,
    });
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
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

    const request$ = id
      ? this.toppingService.update(id, value)
      : this.toppingService.create(value);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.loadToppings();
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Lưu topping thất bại.'));
      },
    });
  }

  deleteTopping(topping: Topping): void {
    if (!confirm(`Xóa topping "${topping.name}"?`)) {
      return;
    }

    this.toppingService.delete(topping.id).subscribe({
      next: () => this.loadToppings(),
      error: (err) => {
        alert(err?.error?.message ?? 'Xóa topping thất bại.');
      },
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  }

  private loadToppings(): void {
    this.loading.set(true);
    this.toppingService.list(false).subscribe({
      next: (toppings) => {
        this.toppings.set(toppings);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Không thể tải danh sách topping.');
        this.loading.set(false);
      },
    });
  }
}
