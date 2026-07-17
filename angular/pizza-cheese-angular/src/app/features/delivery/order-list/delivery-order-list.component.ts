import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { auditTime } from 'rxjs';

import { DeliveryService } from '../../../core/services/delivery.service';
import { OrderRealtimeService } from '../../../core/services/order-realtime.service';
import {
  Order,
  ORDER_STATUS_LABELS,
  OrderStatus,
} from '../../../core/models/order.model';
import { formatVnd } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { enumEquals, getEnumLabel } from '../../../core/utils/coded-enum.util';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

type StatusFilter = OrderStatus | 'ALL';

@Component({
  selector: 'app-delivery-order-list',
  imports: [RouterLink, DatePipe, PaginationComponent],
  templateUrl: './delivery-order-list.component.html',
  styleUrl: './delivery-order-list.component.scss',
})
export class DeliveryOrderListComponent implements OnInit {
  private readonly deliveryService = inject(DeliveryService);
  private readonly orderRealtime = inject(OrderRealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly activeFilter = signal<StatusFilter>('READY');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly actionOrderId = signal<string | null>(null);

  readonly pageSize = 10;
  readonly formatPrice = formatVnd;
  readonly getStatusLabel = (order: Order) => getEnumLabel(order.status, ORDER_STATUS_LABELS);

  readonly filters: { value: StatusFilter; label: string }[] = [
    { value: 'READY', label: 'Chờ giao' },
    { value: 'OUT_FOR_DELIVERY', label: 'Đang giao' },
    { value: 'DELIVERED', label: 'Đã giao' },
    { value: 'ALL', label: 'Tất cả' },
  ];

  ngOnInit(): void {
    this.reload(true);

    this.orderRealtime
      .connect('delivery')
      .pipe(auditTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload(false));
  }

  setFilter(filter: StatusFilter): void {
    this.activeFilter.set(filter);
    this.page.set(0);
    this.reload(true);
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.reload(true);
  }

  needsStartAction(order: Order): boolean {
    return enumEquals(order.status, 'READY');
  }

  needsDeliveredAction(order: Order): boolean {
    return enumEquals(order.status, 'OUT_FOR_DELIVERY');
  }

  startDelivery(order: Order, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.runAction(order.id, () => this.deliveryService.startDelivery(order.id));
  }

  markDelivered(order: Order, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.runAction(order.id, () => this.deliveryService.markDelivered(order.id));
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

  private runAction(orderId: string, action: () => ReturnType<DeliveryService['startDelivery']>): void {
    this.actionOrderId.set(orderId);
    this.errorMessage.set(null);

    action().subscribe({
      next: () => {
        this.actionOrderId.set(null);
        this.reload(true);
      },
      error: (err: HttpErrorResponse) => {
        this.actionOrderId.set(null);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể cập nhật đơn.'));
      },
    });
  }

  private reload(showLoading = true): void {
    if (showLoading) {
      this.loading.set(true);
    }

    this.deliveryService
      .getOrders({
        status: this.resolveStatusParam(this.activeFilter()),
        page: this.page(),
        size: this.pageSize,
      })
      .subscribe({
        next: (result) => {
          this.orders.set(result.content);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tải danh sách đơn.'));
        },
      });
  }

  private resolveStatusParam(filter: StatusFilter): OrderStatus | undefined {
    return filter === 'ALL' ? undefined : filter;
  }
}
