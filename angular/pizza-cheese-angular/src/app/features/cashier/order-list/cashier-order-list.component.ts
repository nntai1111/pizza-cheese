import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { CashierService } from '../../../core/services/cashier.service';
import { ShopContextService } from '../../../core/services/shop-context.service';
import {
  Order,
  ORDER_STATUS_LABELS,
  PAYMENT_METHOD_LABELS,
  PAYMENT_STATUS_LABELS,
  OrderStatus,
} from '../../../core/models/order.model';
import { formatVnd } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { enumEquals, getEnumLabel } from '../../../core/utils/coded-enum.util';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

type StatusFilter = OrderStatus | 'ALL';

@Component({
  selector: 'app-cashier-order-list',
  imports: [RouterLink, DatePipe, PaginationComponent],
  templateUrl: './cashier-order-list.component.html',
  styleUrl: './cashier-order-list.component.scss',
})
export class CashierOrderListComponent {
  private readonly cashierService = inject(CashierService);
  private readonly shopContext = inject(ShopContextService);

  readonly shop = this.shopContext;
  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly activeFilter = signal<StatusFilter>('ALL');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly pageSize = 10;
  readonly formatPrice = formatVnd;
  readonly statusLabels = ORDER_STATUS_LABELS;
  readonly paymentLabels = PAYMENT_METHOD_LABELS;
  readonly paymentStatusLabels = PAYMENT_STATUS_LABELS;
  readonly getStatusLabel = (order: Order) => getEnumLabel(order.status, ORDER_STATUS_LABELS);
  readonly getPaymentLabel = (order: Order) => getEnumLabel(order.paymentMethod, PAYMENT_METHOD_LABELS);
  readonly getPaymentStatusLabel = (order: Order) =>
    order.paymentStatus ? getEnumLabel(order.paymentStatus, PAYMENT_STATUS_LABELS) : '';

  readonly filters: { value: StatusFilter; label: string }[] = [
    { value: 'ALL', label: 'Tất cả' },
    { value: 'PENDING_PAYMENT', label: 'Chờ thanh toán' },
    { value: 'CONFIRMED', label: 'Đã xác nhận' },
    { value: 'CANCELLED', label: 'Đã hủy' },
  ];

  constructor() {
    this.loadOrders('ALL', 0);
  }

  setFilter(filter: StatusFilter): void {
    this.activeFilter.set(filter);
    this.page.set(0);
    this.loadOrders(filter, 0);
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.loadOrders(this.activeFilter(), page);
  }

  needsPaymentAction(order: Order): boolean {
    return enumEquals(order.paymentStatus, 'PENDING')
      && (enumEquals(order.status, 'PENDING_PAYMENT') || enumEquals(order.status, 'CONFIRMED'));
  }

  private loadOrders(filter: StatusFilter, page: number): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.cashierService
      .getOrders({
        status: filter === 'ALL' ? undefined : filter,
        page,
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
}
