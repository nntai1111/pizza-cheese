import { Component, inject, signal } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { ComboService } from '../../../core/services/combo.service';
import { PizzaService } from '../../../core/services/pizza.service';
import { ToastService } from '../../../core/services/toast.service';
import {
  Combo,
  CreateComboRequest,
  ComboItemRequest,
} from '../../../core/models/combo.model';
import { Pizza, PizzaVariant } from '../../../core/models/pizza.model';
import {
  formatComboPrice,
  getComboImageUrl,
  isComboActive,
} from '../../../core/utils/combo.util';
import { getPizzaSizeLabel } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import {
  fieldErrorMessage,
  markFormInvalidAndMessage,
  showFieldError,
} from '../../../core/utils/form-validation.util';

const COMBO_FIELD_LABELS: Record<string, string> = {
  name: 'Tên combo',
  price: 'Giá combo',
  items: 'Pizza trong combo',
  pizzaId: 'Pizza',
  pizzaVariantId: 'Size',
  quantity: 'Số lượng',
};

@Component({
  selector: 'app-combo-list',
  imports: [ReactiveFormsModule],
  templateUrl: './combo-list.component.html',
  styleUrl: './combo-list.component.scss',
})
export class ComboListComponent {
  private readonly fb = inject(FormBuilder);
  private readonly comboService = inject(ComboService);
  private readonly pizzaService = inject(PizzaService);
  private readonly toast = inject(ToastService);

  readonly combos = signal<Combo[]>([]);
  readonly pizzas = signal<Pizza[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly selectedImageFile = signal<File | null>(null);
  readonly imagePreviewUrl = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    slug: ['', Validators.maxLength(150)],
    description: [''],
    price: [null as number | null, [Validators.required, Validators.min(0.01)]],
    discountPercent: [null as number | null],
    isActive: [true],
    items: this.fb.array([this.createItemGroup()]),
  });

  readonly resolveComboImageUrl = getComboImageUrl;
  readonly isComboActive = isComboActive;
  readonly formatPrice = formatComboPrice;
  readonly getSizeLabel = getPizzaSizeLabel;

  constructor() {
    this.loadCombos();
    this.loadPizzas();
  }

  get itemsFormArray(): FormArray {
    return this.form.controls.items;
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

  itemError(index: number, field: string): string | null {
    return fieldErrorMessage(
      this.itemsFormArray.at(index).get(field),
      this.itemMessagesFor(field),
    );
  }

  itemControlClass(index: number, field: string): string {
    return showFieldError(this.itemsFormArray.at(index).get(field))
      ? 'is-invalid'
      : '';
  }

  openCreate(): void {
    this.editingId.set(null);
    this.selectedImageFile.set(null);
    this.imagePreviewUrl.set(null);
    this.form.reset({
      name: '',
      slug: '',
      description: '',
      price: null,
      discountPercent: null,
      isActive: true,
    });
    this.itemsFormArray.clear();
    this.itemsFormArray.push(this.createItemGroup());
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  openEdit(combo: Combo): void {
    this.editingId.set(combo.id);
    this.selectedImageFile.set(null);
    this.imagePreviewUrl.set(getComboImageUrl(combo));
    this.form.patchValue({
      name: combo.name,
      slug: combo.slug,
      description: combo.description ?? '',
      price: combo.price,
      discountPercent: combo.discountPercent,
      isActive: isComboActive(combo),
    });
    this.itemsFormArray.clear();
    combo.items.forEach((item) => {
      this.itemsFormArray.push(
        this.createItemGroup(item.pizzaId, item.pizzaVariantId, item.quantity),
      );
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

  addItemRow(): void {
    this.itemsFormArray.push(this.createItemGroup());
  }

  removeItemRow(index: number): void {
    if (this.itemsFormArray.length <= 1) {
      return;
    }
    this.itemsFormArray.removeAt(index);
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
      this.toast.error(markFormInvalidAndMessage(this.form, COMBO_FIELD_LABELS));
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);
    const value = this.form.getRawValue();
    const id = this.editingId();
    const payload = this.toRequestPayload({
      ...value,
      price: Number(value.price),
    });
    const imageFile = this.selectedImageFile();

    const request$ = id
      ? this.comboService.update(id, payload, imageFile)
      : this.comboService.create(payload, imageFile);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.cancelForm();
        this.loadCombos();
        this.toast.success(id ? 'Đã cập nhật combo' : 'Đã tạo combo');
      },
      error: (err) => {
        this.saving.set(false);
        const message = getHttpErrorMessage(err, 'Lưu combo thất bại.');
        this.errorMessage.set(message);
        this.toast.error(message);
      },
    });
  }

  deleteCombo(combo: Combo): void {
    if (!confirm(`Xóa combo "${combo.name}"?`)) {
      return;
    }

    this.comboService.delete(combo.id).subscribe({
      next: () => {
        this.loadCombos();
        this.toast.success('Đã xóa combo');
      },
      error: (err) => {
        this.toast.error(getHttpErrorMessage(err, 'Xóa combo thất bại.'));
      },
    });
  }

  onPizzaChange(index: number): void {
    const group = this.itemsFormArray.at(index);
    const pizzaId = group.get('pizzaId')?.value;
    const pizza = this.pizzas().find((p) => p.id === pizzaId);
    const firstVariant = pizza?.variants?.[0];
    group.patchValue({
      pizzaVariantId: firstVariant?.id ?? '',
    });
  }

  variantsForPizza(pizzaId: string): PizzaVariant[] {
    return this.pizzas().find((p) => p.id === pizzaId)?.variants ?? [];
  }

  private messagesFor(name: string): Partial<Record<string, string>> {
    if (name === 'name') {
      return { required: 'Vui lòng nhập tên combo' };
    }
    if (name === 'price') {
      return {
        required: 'Vui lòng nhập giá',
        min: 'Giá phải lớn hơn 0',
      };
    }
    return {};
  }

  private itemMessagesFor(field: string): Partial<Record<string, string>> {
    if (field === 'pizzaId') {
      return { required: 'Vui lòng chọn pizza' };
    }
    if (field === 'pizzaVariantId') {
      return { required: 'Vui lòng chọn size' };
    }
    if (field === 'quantity') {
      return {
        required: 'Vui lòng nhập số lượng',
        min: 'Số lượng tối thiểu là 1',
      };
    }
    return {};
  }

  private createItemGroup(
    pizzaId = '',
    pizzaVariantId = '',
    quantity = 1,
  ) {
    return this.fb.nonNullable.group({
      pizzaId: [pizzaId, Validators.required],
      pizzaVariantId: [pizzaVariantId, Validators.required],
      quantity: [quantity, [Validators.required, Validators.min(1)]],
    });
  }

  private toRequestPayload(value: {
    name: string;
    slug: string;
    description: string;
    price: number;
    discountPercent: number | null;
    isActive: boolean;
    items: ComboItemRequest[];
  }): CreateComboRequest {
    const payload: CreateComboRequest = {
      name: value.name.trim(),
      price: value.price,
      isActive: value.isActive,
      items: value.items.map((item) => ({
        pizzaId: item.pizzaId,
        pizzaVariantId: item.pizzaVariantId,
        quantity: item.quantity,
      })),
    };

    const slug = value.slug.trim();
    if (slug) {
      payload.slug = slug;
    }

    const description = value.description.trim();
    if (description) {
      payload.description = description;
    }

    if (value.discountPercent != null && value.discountPercent >= 0) {
      payload.discountPercent = value.discountPercent;
    }

    return payload;
  }

  private revokePreviewUrl(): void {
    const preview = this.imagePreviewUrl();
    if (preview?.startsWith('blob:')) {
      URL.revokeObjectURL(preview);
    }
  }

  private loadCombos(): void {
    this.loading.set(true);
    this.comboService.list({ activeOnly: false, page: 0, size: 100 }).subscribe({
      next: (result) => {
        this.combos.set(result.content);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Không thể tải danh sách combo.');
        this.loading.set(false);
      },
    });
  }

  private loadPizzas(): void {
    this.pizzaService
      .list({ activeOnly: true, page: 0, size: 100 })
      .subscribe({
        next: (result) => this.pizzas.set(result.content),
      });
  }
}
