import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { CartService } from '../../../core/services/cart.service';
import { OrderService } from '../../../core/services/order.service';
import { PaymentMethod } from '../../../core/models/order.model';
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
  private readonly router = inject(Router);

  readonly cart = this.cartService.cart;
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

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
    paymentMethod: ['VNPAY' as PaymentMethod, Validators.required],
  });

  constructor() {
    this.cartService.loadCart().subscribe({
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tải giỏ hàng.'));
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const cart = this.cart();
    if (!cart?.items.length) {
      this.errorMessage.set('Giỏ hàng trống.');
      return;
    }

    const value = this.form.getRawValue();
    this.loading.set(true);
    this.errorMessage.set(null);

    this.orderService
      .createOrder({
        paymentMethod: value.paymentMethod,
        note: value.note || undefined,
        deliveryAddress: {
          recipientName: value.recipientName,
          phone: value.phone,
          addressLine1: value.addressLine1,
          addressLine2: value.addressLine2 || undefined,
          ward: value.ward || undefined,
          district: value.district || undefined,
          city: value.city,
        },
      })
      .subscribe({
        next: (order) => {
          this.loading.set(false);
          this.cartService.loadCart().subscribe();
          sessionStorage.setItem(PENDING_ORDER_KEY, order.id);
          sessionStorage.setItem('pizza_cheese_last_order', JSON.stringify(order));

          if (value.paymentMethod === 'VNPAY' && order.paymentUrl) {
            // VNPay dùng redirect URL, không gọi fetch/XHR tới sandbox
            window.location.assign(order.paymentUrl);
            return;
          }

          void this.router.navigate(['/customer/orders', order.id]);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tạo đơn hàng.'));
        },
      });
  }
}

export { PENDING_ORDER_KEY };
