import { Component, inject, signal } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { ComboService } from '../../../core/services/combo.service';
import { PizzaService } from '../../../core/services/pizza.service';
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
    price: [0, [Validators.required, Validators.min(0.01)]],
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

  openCreate(): void {
    this.editingId.set(null);
    this.selectedImageFile.set(null);
    this.imagePreviewUrl.set(null);
    this.form.reset({
      name: '',
      slug: '',
      description: '',
      price: 0,
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
      ? this.comboService.update(id, payload, imageFile)
      : this.comboService.create(payload, imageFile);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.cancelForm();
        this.loadCombos();
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Lưu combo thất bại.'));
      },
    });
  }

  deleteCombo(combo: Combo): void {
    if (!confirm(`Xóa combo "${combo.name}"?`)) {
      return;
    }

    this.comboService.delete(combo.id).subscribe({
      next: () => this.loadCombos(),
      error: (err) => {
        alert(getHttpErrorMessage(err, 'Xóa combo thất bại.'));
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
