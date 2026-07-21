import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { CashierService } from '../../../core/services/cashier.service';
import {
  Order,
  ORDER_STATUS_LABELS,
  PAYMENT_METHOD_LABELS,
  PAYMENT_STATUS_LABELS,
  OrderStatus,
} from '../../../core/models/order.model';
import { formatVnd } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { getEnumLabel } from '../../../core/utils/coded-enum.util';
import {
  orderStatusTone,
  paymentStatusTone,
  statusBadgeClass,
} from '../../../core/utils/status-tone.util';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

type StatusFilter = OrderStatus | 'ALL';

const STATUS_FILTERS: StatusFilter[] = [
  'ALL',
  'PENDING_PAYMENT',
  'CONFIRMED',
  'PREPARING',
  'READY',
  'OUT_FOR_DELIVERY',
  'COMPLETED',
  'CANCELLED',
];

@Component({
  selector: 'app-admin-order-list',
  imports: [DatePipe, FormsModule, PaginationComponent],
  templateUrl: './admin-order-list.component.html',
  styleUrl: './admin-order-list.component.scss',
})
export class AdminOrderListComponent {
  private readonly cashierService = inject(CashierService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly activeFilter = signal<StatusFilter>('ALL');
  readonly fromDate = signal('');
  readonly toDate = signal('');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly pageSize = 10;
  readonly formatPrice = formatVnd;
  readonly getStatusLabel = (order: Order) => getEnumLabel(order.status, ORDER_STATUS_LABELS);
  readonly getPaymentLabel = (order: Order) => getEnumLabel(order.paymentMethod, PAYMENT_METHOD_LABELS);
  readonly getPaymentStatusLabel = (order: Order) =>
    order.paymentStatus ? getEnumLabel(order.paymentStatus, PAYMENT_STATUS_LABELS) : '';
  readonly orderBadgeClass = (order: Order) => statusBadgeClass(orderStatusTone(order.status));
  readonly paymentBadgeClass = (order: Order) =>
    statusBadgeClass(paymentStatusTone(order.paymentStatus));

  readonly filters: { value: StatusFilter; label: string }[] = [
    { value: 'ALL', label: 'Tất cả' },
    { value: 'PENDING_PAYMENT', label: 'Chờ thanh toán' },
    { value: 'CONFIRMED', label: 'Đã xác nhận' },
    { value: 'PREPARING', label: 'Đang chế biến' },
    { value: 'READY', label: 'Sẵn sàng' },
    { value: 'OUT_FOR_DELIVERY', label: 'Đang giao' },
    { value: 'COMPLETED', label: 'Hoàn thành' },
    { value: 'CANCELLED', label: 'Đã hủy' },
  ];

  constructor() {
    const statusParam = this.route.snapshot.queryParamMap.get('status');
    if (statusParam && STATUS_FILTERS.includes(statusParam as StatusFilter)) {
      this.activeFilter.set(statusParam as StatusFilter);
    }
    this.reload();
  }

  openOrder(order: Order): void {
    this.router.navigate(['/admin/orders', order.id]);
  }

  setFilter(filter: StatusFilter): void {
    this.activeFilter.set(filter);
    this.page.set(0);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: filter === 'ALL' ? {} : { status: filter },
      replaceUrl: true,
    });
    this.reload();
  }

  applyDateFilter(): void {
    this.page.set(0);
    this.reload();
  }

  clearDateFilter(): void {
    this.fromDate.set('');
    this.toDate.set('');
    this.page.set(0);
    this.reload();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.reload();
  }

  private reload(): void {
    this.loadOrders(this.activeFilter(), this.page());
  }

  private loadOrders(filter: StatusFilter, page: number): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.cashierService
      .getOrders({
        status: filter === 'ALL' ? undefined : filter,
        from: this.fromDate() || undefined,
        to: this.toDate() || undefined,
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
