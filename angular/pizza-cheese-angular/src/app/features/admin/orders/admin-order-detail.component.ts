import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { CashierService } from '../../../core/services/cashier.service';
import {
  Order,
  ORDER_STATUS_LABELS,
  PAYMENT_METHOD_LABELS,
  PAYMENT_STATUS_LABELS,
} from '../../../core/models/order.model';
import { formatVnd, getPizzaSizeLabel } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { enumEquals, getEnumLabel } from '../../../core/utils/coded-enum.util';

@Component({
  selector: 'app-admin-order-detail',
  imports: [RouterLink, DatePipe],
  templateUrl: './admin-order-detail.component.html',
  styleUrls: [
    '../../customer/order-detail/order-detail.component.scss',
    './admin-order-detail.component.scss',
  ],
})
export class AdminOrderDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly cashierService = inject(CashierService);

  readonly order = signal<Order | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly formatPrice = formatVnd;
  readonly getStatusLabel = (order: Order) => getEnumLabel(order.status, ORDER_STATUS_LABELS);
  readonly getPaymentLabel = (order: Order) => getEnumLabel(order.paymentMethod, PAYMENT_METHOD_LABELS);
  readonly getPaymentStatusLabel = (order: Order) =>
    order.paymentStatus ? getEnumLabel(order.paymentStatus, PAYMENT_STATUS_LABELS) : '';

  constructor() {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (!orderId) {
      this.loading.set(false);
      this.errorMessage.set('Không tìm thấy đơn hàng.');
      return;
    }

    this.loadOrder(orderId);
  }

  getDeliveryAddress(order: Order): string {
    try {
      const address = JSON.parse(order.deliveryAddressSnapshot) as {
        recipientName?: string;
        phone?: string;
        addressLine1?: string;
        addressLine2?: string;
        ward?: string;
        district?: string;
        city?: string;
      };
      if (address.addressLine1 === 'Tại quầy') {
        return `Mua tại quầy · Khách: ${address.recipientName || 'Khách lẻ'} · SĐT: ${address.phone || 'Không có'}`;
      }
      const parts = [
        address.addressLine1,
        address.addressLine2,
        address.ward,
        address.district,
        address.city,
      ].filter(Boolean);
      return `${address.recipientName ?? ''} · ${address.phone ?? ''} · ${parts.join(', ')}`;
    } catch {
      return order.deliveryAddressSnapshot;
    }
  }

  getItemTitle(item: NonNullable<Order['items']>[number]): string {
    if (enumEquals(item.itemType, 'COMBO')) {
      return item.comboName ?? 'Combo';
    }
    const size = item.pizzaSize ? getPizzaSizeLabel(item.pizzaSize) : '';
    return `${item.pizzaName ?? 'Pizza'}${size ? ` (${size})` : ''}`;
  }

  private loadOrder(orderId: string): void {
    this.cashierService.getOrder(orderId).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tải đơn hàng.'));
      },
    });
  }
}
