import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { OrderService } from '../../../core/services/order.service';
import {
  Order,
  ORDER_STATUS_LABELS,
  PAYMENT_METHOD_LABELS,
} from '../../../core/models/order.model';
import { formatVnd, getPizzaSizeLabel } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';

@Component({
  selector: 'app-order-detail',
  imports: [RouterLink, DatePipe],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.scss',
})
export class OrderDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);

  readonly order = signal<Order | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly cancelling = signal(false);

  readonly formatPrice = formatVnd;
  readonly getSizeLabel = getPizzaSizeLabel;
  readonly statusLabels = ORDER_STATUS_LABELS;
  readonly paymentLabels = PAYMENT_METHOD_LABELS;

  constructor() {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (!orderId) {
      this.loading.set(false);
      this.errorMessage.set('Không tìm thấy đơn hàng.');
      return;
    }

    this.orderService.getOrder(orderId).subscribe({
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

  canCancel(order: Order): boolean {
    return order.status === 'PENDING_PAYMENT'
      || (order.status === 'CONFIRMED' && order.paymentStatus === 'PENDING');
  }

  cancelOrder(): void {
    const current = this.order();
    if (!current) {
      return;
    }

    this.cancelling.set(true);
    this.errorMessage.set(null);

    this.orderService.cancelOrder(current.id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.cancelling.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.cancelling.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể hủy đơn.'));
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
    if (item.itemType === 'COMBO') {
      return item.comboName ?? 'Combo';
    }
    const size = item.pizzaSize ? getPizzaSizeLabel(item.pizzaSize) : '';
    return `${item.pizzaName ?? 'Pizza'}${size ? ` (${size})` : ''}`;
  }
}
