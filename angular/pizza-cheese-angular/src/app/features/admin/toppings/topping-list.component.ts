import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { ToppingService } from '../../../core/services/topping.service';
import { ToastService } from '../../../core/services/toast.service';
import { Topping } from '../../../core/models/topping.model';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import {
  fieldErrorMessage,
  markFormInvalidAndMessage,
  showFieldError,
} from '../../../core/utils/form-validation.util';

const TOPPING_FIELD_LABELS: Record<string, string> = {
  name: 'Tên topping',
  price: 'Giá',
};

@Component({
  selector: 'app-topping-list',
  imports: [ReactiveFormsModule],
  templateUrl: './topping-list.component.html',
  styleUrl: './topping-list.component.scss',
})
export class ToppingListComponent {
  private readonly fb = inject(FormBuilder);
  private readonly toppingService = inject(ToppingService);
  private readonly toast = inject(ToastService);

  readonly toppings = signal<Topping[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    price: [null as number | null, [Validators.required, Validators.min(0)]],
    isActive: [true],
  });

  constructor() {
    this.loadToppings();
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

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', price: null, isActive: true });
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
      this.toast.error(markFormInvalidAndMessage(this.form, TOPPING_FIELD_LABELS));
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);
    const value = this.form.getRawValue();
    const id = this.editingId();
    const payload = {
      name: value.name,
      price: Number(value.price),
      isActive: value.isActive,
    };

    const request$ = id
      ? this.toppingService.update(id, payload)
      : this.toppingService.create(payload);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.loadToppings();
        this.toast.success(id ? 'Đã cập nhật topping' : 'Đã tạo topping');
      },
      error: (err) => {
        this.saving.set(false);
        const message = getHttpErrorMessage(err, 'Lưu topping thất bại.');
        this.errorMessage.set(message);
        this.toast.error(message);
      },
    });
  }

  deleteTopping(topping: Topping): void {
    if (!confirm(`Xóa topping "${topping.name}"?`)) {
      return;
    }

    this.toppingService.delete(topping.id).subscribe({
      next: () => {
        this.loadToppings();
        this.toast.success('Đã xóa topping');
      },
      error: (err) => {
        this.toast.error(getHttpErrorMessage(err, 'Xóa topping thất bại.'));
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
    if (name === 'name') {
      return { required: 'Vui lòng nhập tên topping' };
    }
    if (name === 'price') {
      return {
        required: 'Vui lòng nhập giá',
        min: 'Giá không được âm',
      };
    }
    return {};
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
