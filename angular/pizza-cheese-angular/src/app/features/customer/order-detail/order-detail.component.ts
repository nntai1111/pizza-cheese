import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { OrderService } from '../../../core/services/order.service';
import {
  Order,
  OrderStatus,
  ORDER_STATUS_LABELS,
  PAYMENT_METHOD_LABELS,
} from '../../../core/models/order.model';
import { formatVnd, getPizzaSizeLabel } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { codedEnumLabel, codedEnumName, enumEquals, getEnumLabel } from '../../../core/utils/coded-enum.util';

type StepState = 'done' | 'current' | 'upcoming';

interface TrackStep {
  key: string;
  title: string;
  description: string;
  state: StepState;
}

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
  readonly getStatusLabel = (order: Order) => getEnumLabel(order.status, ORDER_STATUS_LABELS);
  readonly getPaymentLabel = (order: Order) => getEnumLabel(order.paymentMethod, PAYMENT_METHOD_LABELS);
  readonly getPaymentStatusLabel = (order: Order) => codedEnumLabel(order.paymentStatus);

  readonly isTerminal = computed(() => {
    const order = this.order();
    if (!order) return false;
    return enumEquals(order.status, 'CANCELLED') || enumEquals(order.status, 'REFUNDED');
  });

  readonly isPickup = computed(() => {
    const order = this.order();
    if (!order) return false;
    try {
      const address = JSON.parse(order.deliveryAddressSnapshot) as { addressLine1?: string };
      return address.addressLine1 === 'Tại quầy';
    } catch {
      return false;
    }
  });

  readonly headline = computed(() => {
    const order = this.order();
    if (!order) return { title: '', subtitle: '' };
    if (enumEquals(order.status, 'CANCELLED')) {
      return { title: 'Đơn đã hủy', subtitle: 'Đơn hàng này không còn được xử lý.' };
    }
    if (enumEquals(order.status, 'REFUNDED')) {
      return { title: 'Đã hoàn tiền', subtitle: 'Đơn đã được hoàn tiền.' };
    }

    const pickup = this.isPickup();
    const status = codedEnumName(order.status);
    switch (status) {
      case 'PENDING_PAYMENT':
        return { title: 'Chờ thanh toán', subtitle: 'Hoàn tất thanh toán để cửa hàng bắt đầu xử lý đơn.' };
      case 'CONFIRMED':
        return { title: 'Đã xác nhận', subtitle: 'Cửa hàng đã nhận đơn và chuẩn bị đưa vào bếp.' };
      case 'PREPARING':
        return { title: 'Bếp đang làm', subtitle: 'Pizza của bạn đang được chế biến.' };
      case 'READY':
        return {
          title: pickup ? 'Sẵn sàng nhận' : 'Sẵn sàng giao',
          subtitle: pickup
            ? 'Đơn đã xong — bạn có thể đến quầy nhận.'
            : 'Đơn đã xong — đang chờ shipper nhận giao.',
        };
      case 'OUT_FOR_DELIVERY':
        return { title: 'Đang giao tới bạn', subtitle: 'Shipper đang trên đường giao đơn.' };
      case 'COMPLETED':
        return {
          title: pickup ? 'Đã lấy hàng' : 'Đã giao thành công',
          subtitle: 'Cảm ơn bạn — chúc ngon miệng!',
        };
      default:
        return { title: this.getStatusLabel(order), subtitle: '' };
    }
  });

  readonly trackSteps = computed((): TrackStep[] => {
    const order = this.order();
    if (!order || this.isTerminal()) return [];

    const pickup = this.isPickup();
    const status = codedEnumName(order.status);
    const currentIndex = this.stepIndex(status);

    const shippingTitle = pickup
      ? 'Sẵn sàng nhận'
      : status === 'OUT_FOR_DELIVERY'
        ? 'Đang giao tới bạn'
        : 'Chờ giao hàng';
    const shippingDesc = pickup
      ? 'Đơn đã sẵn sàng tại quầy.'
      : status === 'OUT_FOR_DELIVERY'
        ? 'Shipper đang mang đơn đến địa chỉ của bạn.'
        : 'Đơn đã xong bếp, đang chờ shipper nhận giao.';

    const definitions = [
      {
        key: 'placed',
        title: 'Đã đặt đơn',
        description: 'Chúng tôi đã nhận yêu cầu đặt hàng của bạn.',
      },
      {
        key: 'confirmed',
        title: 'Đã xác nhận',
        description: 'Cửa hàng đã xác nhận đơn.',
      },
      {
        key: 'preparing',
        title: 'Bếp đang làm',
        description: 'Đội bếp đang chế biến món của bạn.',
      },
      {
        key: 'shipping',
        title: shippingTitle,
        description: shippingDesc,
      },
      {
        key: 'done',
        title: pickup ? 'Đã lấy hàng' : 'Đã giao',
        description: 'Đơn hoàn tất. Cảm ơn bạn!',
      },
    ];

    return definitions.map((step, index) => ({
      ...step,
      state: index < currentIndex ? 'done' : index === currentIndex ? 'current' : 'upcoming',
    }));
  });

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
    return (
      enumEquals(order.status, 'PENDING_PAYMENT') ||
      (enumEquals(order.status, 'CONFIRMED') && enumEquals(order.paymentStatus, 'PENDING'))
    );
  }

  cancelOrder(): void {
    const current = this.order();
    if (!current) return;

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
      if (address.addressLine1 === 'Tại quầy') {
        return `Nhận tại quầy · ${address.recipientName || 'Khách'} · ${address.phone || 'Không có SĐT'}`;
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

  getItemImage(item: NonNullable<Order['items']>[number]): string | null {
    if (enumEquals(item.itemType, 'COMBO')) {
      return item.comboImageUrl;
    }
    return item.pizzaImageUrl;
  }

  getToppingNames(item: NonNullable<Order['items']>[number]): string {
    if (!item.toppings?.length) return '';
    return item.toppings.map((t) => t.toppingName).join(', ');
  }

  /** Map backend status → customer-facing step index (0..4). */
  private stepIndex(status: OrderStatus | null): number {
    switch (status) {
      case 'PENDING_PAYMENT':
        return 0;
      case 'CONFIRMED':
        return 1;
      case 'PREPARING':
        return 2;
      case 'READY':
      case 'OUT_FOR_DELIVERY':
        return 3;
      case 'COMPLETED':
        return 4;
      default:
        return 0;
    }
  }
}
