import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CartService } from '../../../core/services/cart.service';
import { CashierService } from '../../../core/services/cashier.service';
import { OrderService } from '../../../core/services/order.service';
import { ShopContextService } from '../../../core/services/shop-context.service';
import { Order, PaymentMethod } from '../../../core/models/order.model';
import { formatVnd } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import {
  getCartItemImage,
  getCartItemTitle,
} from '../../../core/utils/cart-display.util';

const PENDING_ORDER_KEY = 'pizza_cheese_pending_order_id';

@Component({
  selector: 'app-checkout',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss',
})
export class CheckoutComponent {
  private readonly fb = inject(FormBuilder);
  private readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly cashierService = inject(CashierService);
  private readonly shopContext = inject(ShopContextService);
  private readonly router = inject(Router);

  readonly shop = this.shopContext;
  readonly cart = this.cartService.cart;
  readonly selectedItems = this.cartService.selectedItems;
  readonly selectedSubtotal = this.cartService.selectedSubtotal;
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isCashierMode = signal(this.shopContext.basePath() === '/cashier');

  readonly formatPrice = formatVnd;
  readonly getItemTitle = getCartItemTitle;
  readonly getItemImage = getCartItemImage;

  readonly form = this.fb.nonNullable.group({
    recipientName: ['', [Validators.required, Validators.maxLength(100)]],
    phone: ['', [Validators.required, Validators.maxLength(20)]],
    addressLine1: ['', [Validators.required, Validators.maxLength(255)]],
    addressLine2: [''],
    ward: [''],
    district: [''],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    note: [''],
    paymentMethod: [
      (this.shopContext.basePath() === '/cashier' ? 'COD' : 'VNPAY') as PaymentMethod,
      Validators.required,
    ],
  });

  constructor() {
    this.cartService.loadCart().subscribe({
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tải giỏ hàng.'));
      },
    });

    if (this.isCashierMode()) {
      this.form.controls.recipientName.clearValidators();
      this.form.controls.recipientName.setValidators([Validators.maxLength(100)]);
      this.form.controls.phone.clearValidators();
      this.form.controls.phone.setValidators([Validators.maxLength(20)]);
      this.form.controls.addressLine1.clearValidators();
      this.form.controls.addressLine1.setValidators([Validators.maxLength(255)]);
      this.form.controls.city.clearValidators();
      this.form.controls.city.setValidators([Validators.maxLength(100)]);

      this.form.controls.recipientName.updateValueAndValidity();
      this.form.controls.phone.updateValueAndValidity();
      this.form.controls.addressLine1.updateValueAndValidity();
      this.form.controls.city.updateValueAndValidity();
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const cart = this.cart();
    const selectedItems = this.selectedItems();
    if (!cart?.items.length) {
      this.errorMessage.set('Giỏ hàng trống.');
      return;
    }
    if (!selectedItems.length) {
      this.errorMessage.set('Vui lòng chọn ít nhất một món để thanh toán.');
      return;
    }

    const value = this.form.getRawValue();
    this.loading.set(true);
    this.errorMessage.set(null);

    const isCashier = this.isCashierMode();
    const recipientName = isCashier ? (value.recipientName.trim() || 'Khách lẻ') : value.recipientName;
    const phone = isCashier ? (value.phone.trim() || '0000000000') : value.phone;
    const addressLine1 = isCashier ? 'Tại quầy' : value.addressLine1;
    const city = isCashier ? 'Hà Nội' : value.city;

    const request = {
      cartItemIds: selectedItems.map((item) => item.id),
      paymentMethod: value.paymentMethod,
      note: value.note || undefined,
      deliveryAddress: {
        recipientName,
        phone,
        addressLine1,
        addressLine2: isCashier ? undefined : (value.addressLine2 || undefined),
        ward: isCashier ? undefined : (value.ward || undefined),
        district: isCashier ? undefined : (value.district || undefined),
        city,
      },
    };

    const createOrder$: Observable<Order> = this.isCashierMode()
      ? this.cashierService.createOrder(request)
      : this.orderService.createOrder(request);

    createOrder$.subscribe({
      next: (order) => {
        this.loading.set(false);
        this.cartService.clearCheckoutSelection();
        this.cartService.loadCart().subscribe();
        sessionStorage.setItem(PENDING_ORDER_KEY, order.id);
        sessionStorage.setItem('pizza_cheese_last_order', JSON.stringify(order));

        if (value.paymentMethod === 'VNPAY' && order.paymentUrl) {
          window.location.assign(order.paymentUrl);
          return;
        }

        void this.router.navigate(this.shopContext.segments('orders', order.id));
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tạo đơn hàng.'));
      },
    });
  }
}

export { PENDING_ORDER_KEY };
