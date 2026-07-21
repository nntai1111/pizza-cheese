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
import {
  orderStatusTone,
  paymentStatusTone,
  statusBadgeClass,
} from '../../../core/utils/status-tone.util';

interface DeliveryInfo {
  isPickup: boolean;
  recipientName: string;
  phone: string;
  addressLines: string[];
  raw: string;
}

@Component({
  selector: 'app-admin-order-detail',
  imports: [RouterLink, DatePipe],
  templateUrl: './admin-order-detail.component.html',
  styleUrl: './admin-order-detail.component.scss',
})
export class AdminOrderDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly cashierService = inject(CashierService);

  readonly order = signal<Order | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionLoading = signal(false);

  readonly formatPrice = formatVnd;
  readonly getStatusLabel = (order: Order) => getEnumLabel(order.status, ORDER_STATUS_LABELS);
  readonly getPaymentLabel = (order: Order) => getEnumLabel(order.paymentMethod, PAYMENT_METHOD_LABELS);
  readonly getPaymentStatusLabel = (order: Order) =>
    order.paymentStatus ? getEnumLabel(order.paymentStatus, PAYMENT_STATUS_LABELS) : '—';
  readonly orderToneClass = (order: Order) =>
    `status-panel status-panel--${orderStatusTone(order.status)}`;
  readonly paymentToneClass = (order: Order) =>
    `status-panel status-panel--${paymentStatusTone(order.paymentStatus)}`;
  readonly orderBadgeClass = (order: Order) =>
    `${statusBadgeClass(orderStatusTone(order.status))} status-badge--lg`;
  readonly paymentBadgeClass = (order: Order) =>
    `${statusBadgeClass(paymentStatusTone(order.paymentStatus))} status-badge--lg`;

  constructor() {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (!orderId) {
      this.loading.set(false);
      this.errorMessage.set('Không tìm thấy đơn hàng.');
      return;
    }

    this.loadOrder(orderId);
  }

  canCancel(order: Order): boolean {
    return (
      enumEquals(order.status, 'PENDING_PAYMENT') ||
      (enumEquals(order.status, 'CONFIRMED') && enumEquals(order.paymentStatus, 'PENDING'))
    );
  }

  cancelOrder(): void {
    const current = this.order();
    if (!current || !confirm(`Hủy đơn ${current.orderCode}?`)) {
      return;
    }

    this.actionLoading.set(true);
    this.errorMessage.set(null);

    this.cashierService.cancelOrder(current.id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.actionLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.actionLoading.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể hủy đơn.'));
      },
    });
  }

  getDeliveryInfo(order: Order): DeliveryInfo {
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
      const isPickup = address.addressLine1 === 'Tại quầy';
      const addressLines = isPickup
        ? ['Nhận tại quầy']
        : [address.addressLine1, address.addressLine2, address.ward, address.district, address.city].filter(
            (part): part is string => !!part,
          );
      return {
        isPickup,
        recipientName: address.recipientName || '—',
        phone: address.phone || '—',
        addressLines,
        raw: order.deliveryAddressSnapshot,
      };
    } catch {
      return {
        isPickup: false,
        recipientName: '—',
        phone: '—',
        addressLines: [order.deliveryAddressSnapshot],
        raw: order.deliveryAddressSnapshot,
      };
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
