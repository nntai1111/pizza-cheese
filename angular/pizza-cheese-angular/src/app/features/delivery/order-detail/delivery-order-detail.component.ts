import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { DeliveryService } from '../../../core/services/delivery.service';
import { Order, ORDER_STATUS_LABELS, PAYMENT_METHOD_LABELS } from '../../../core/models/order.model';
import { formatVnd, getPizzaSizeLabel } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { enumEquals, getEnumLabel } from '../../../core/utils/coded-enum.util';

@Component({
  selector: 'app-delivery-order-detail',
  imports: [RouterLink, DatePipe],
  templateUrl: './delivery-order-detail.component.html',
  styleUrls: [
    '../../customer/order-detail/order-detail.component.scss',
    './delivery-order-detail.component.scss',
  ],
})
export class DeliveryOrderDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly deliveryService = inject(DeliveryService);

  readonly order = signal<Order | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionLoading = signal(false);

  readonly formatPrice = formatVnd;
  readonly getSizeLabel = getPizzaSizeLabel;
  readonly getStatusLabel = (order: Order) => getEnumLabel(order.status, ORDER_STATUS_LABELS);
  readonly getPaymentMethodLabel = (order: Order) =>
    getEnumLabel(order.paymentMethod, PAYMENT_METHOD_LABELS);

  constructor() {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (!orderId) {
      this.loading.set(false);
      this.errorMessage.set('Không tìm thấy đơn hàng.');
      return;
    }

    this.loadOrder(orderId);
  }

  canStartDelivery(order: Order): boolean {
    return enumEquals(order.status, 'READY');
  }

  canMarkDelivered(order: Order): boolean {
    return enumEquals(order.status, 'OUT_FOR_DELIVERY');
  }

  startDelivery(): void {
    const current = this.order();
    if (!current) {
      return;
    }

    this.actionLoading.set(true);
    this.errorMessage.set(null);

    this.deliveryService.startDelivery(current.id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.actionLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.actionLoading.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể nhận đơn giao.'));
      },
    });
  }

  markDelivered(): void {
    const current = this.order();
    if (!current) {
      return;
    }

    this.actionLoading.set(true);
    this.errorMessage.set(null);

    this.deliveryService.markDelivered(current.id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.actionLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.actionLoading.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể đánh dấu đã giao.'));
      },
    });
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
        return `Mua tại quầy · ${address.recipientName || 'Khách lẻ'}`;
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

  getToppingSummary(item: NonNullable<Order['items']>[number]): string | null {
    if (!item.toppings?.length) {
      return null;
    }
    return item.toppings.map((t) => t.toppingName).join(', ');
  }

  getComboLineSummary(item: NonNullable<Order['items']>[number]): string | null {
    if (!item.comboLines?.length) {
      return null;
    }
    return item.comboLines
      .map((line) => `${line.quantity}× ${line.pizzaName} (${getPizzaSizeLabel(line.pizzaSize)})`)
      .join(' · ');
  }

  private loadOrder(orderId: string): void {
    this.deliveryService.getOrder(orderId).subscribe({
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
