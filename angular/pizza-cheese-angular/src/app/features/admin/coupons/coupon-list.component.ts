import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { CouponService } from '../../../core/services/coupon.service';
import {
  Coupon,
  DISCOUNT_TYPE_LABELS,
  DiscountType,
} from '../../../core/models/coupon.model';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { codedEnumName } from '../../../core/utils/coded-enum.util';

@Component({
  selector: 'app-coupon-list',
  imports: [ReactiveFormsModule],
  templateUrl: './coupon-list.component.html',
  styleUrl: './coupon-list.component.scss',
})
export class CouponListComponent {
  private readonly fb = inject(FormBuilder);
  private readonly couponService = inject(CouponService);

  readonly coupons = signal<Coupon[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly discountTypeLabels = DISCOUNT_TYPE_LABELS;
  readonly discountTypes: DiscountType[] = ['PERCENT', 'FIXED'];

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(50)]],
    description: [''],
    discountType: ['PERCENT' as DiscountType, Validators.required],
    discountValue: [0, [Validators.required, Validators.min(0.01)]],
    minOrderValue: [null as number | null],
    maxDiscount: [null as number | null],
    startDate: [''],
    endDate: [''],
    usageLimit: [null as number | null],
    perUserLimit: [null as number | null],
    isActive: [true],
  });

  constructor() {
    this.loadCoupons();
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({
      code: '',
      description: '',
      discountType: 'PERCENT',
      discountValue: 10,
      minOrderValue: null,
      maxDiscount: null,
      startDate: '',
      endDate: '',
      usageLimit: null,
      perUserLimit: null,
      isActive: true,
    });
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  openEdit(coupon: Coupon): void {
    this.editingId.set(coupon.id);
    this.form.patchValue({
      code: coupon.code,
      description: coupon.description ?? '',
      discountType: codedEnumName(coupon.discountType) ?? 'PERCENT',
      discountValue: coupon.discountValue,
      minOrderValue: coupon.minOrderValue,
      maxDiscount: coupon.maxDiscount,
      startDate: this.toLocalInput(coupon.startDate),
      endDate: this.toLocalInput(coupon.endDate),
      usageLimit: coupon.usageLimit,
      perUserLimit: coupon.perUserLimit,
      isActive: coupon.active,
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
    const payload = {
      code: value.code.trim(),
      description: value.description.trim() || undefined,
      discountType: value.discountType,
      discountValue: value.discountValue,
      minOrderValue: value.minOrderValue ?? undefined,
      maxDiscount: value.maxDiscount ?? undefined,
      startDate: this.fromLocalInput(value.startDate),
      endDate: this.fromLocalInput(value.endDate),
      usageLimit: value.usageLimit ?? undefined,
      perUserLimit: value.perUserLimit ?? undefined,
      isActive: value.isActive,
    };
    const id = this.editingId();

    const request$ = id
      ? this.couponService.update(id, payload)
      : this.couponService.create(payload);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.loadCoupons();
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Lưu coupon thất bại.'));
      },
    });
  }

  deleteCoupon(coupon: Coupon): void {
    if (!confirm(`Vô hiệu hóa coupon "${coupon.code}"?`)) {
      return;
    }

    this.couponService.delete(coupon.id).subscribe({
      next: () => this.loadCoupons(),
      error: (err) => {
        alert(getHttpErrorMessage(err, 'Xóa coupon thất bại.'));
      },
    });
  }

  formatPrice(price: number | null): string {
    if (price == null) {
      return '—';
    }
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  }

  getDiscountLabel(coupon: Coupon): string {
    if (codedEnumName(coupon.discountType) === 'PERCENT') {
      return `${coupon.discountValue}%`;
    }
    return this.formatPrice(coupon.discountValue);
  }

  formatDateRange(coupon: Coupon): string {
    const start = coupon.startDate ? this.formatDate(coupon.startDate) : null;
    const end = coupon.endDate ? this.formatDate(coupon.endDate) : null;
    if (!start && !end) return 'Không giới hạn';
    if (start && end) return `${start} → ${end}`;
    if (start) return `Từ ${start}`;
    return `Đến ${end}`;
  }

  private formatDate(iso: string): string {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return iso;
    return d.toLocaleDateString('vi-VN');
  }

  private toLocalInput(iso: string | null): string {
    if (!iso) return '';
    return iso.length >= 16 ? iso.slice(0, 16) : iso;
  }

  private fromLocalInput(value: string): string | undefined {
    const trimmed = value?.trim();
    if (!trimmed) return undefined;
    return trimmed.length === 16 ? `${trimmed}:00` : trimmed;
  }

  private loadCoupons(): void {
    this.loading.set(true);
    this.couponService.list(false).subscribe({
      next: (coupons) => {
        this.coupons.set(coupons);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Không thể tải danh sách coupon.');
        this.loading.set(false);
      },
    });
  }
}
