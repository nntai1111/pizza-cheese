import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { interval } from 'rxjs';

import { KitchenService } from '../../../core/services/kitchen.service';
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
  selector: 'app-kitchen-order-list',
  imports: [RouterLink, DatePipe, PaginationComponent],
  templateUrl: './kitchen-order-list.component.html',
  styleUrl: './kitchen-order-list.component.scss',
})
export class KitchenOrderListComponent implements OnInit {
  private readonly kitchenService = inject(KitchenService);
  private readonly destroyRef = inject(DestroyRef);

  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly activeFilter = signal<StatusFilter>('CONFIRMED');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly actionOrderId = signal<string | null>(null);

  readonly pageSize = 10;
  readonly formatPrice = formatVnd;
  readonly getStatusLabel = (order: Order) => getEnumLabel(order.status, ORDER_STATUS_LABELS);

  readonly filters: { value: StatusFilter; label: string }[] = [
    { value: 'CONFIRMED', label: 'Chờ làm' },
    { value: 'PREPARING', label: 'Đang làm' },
    { value: 'READY', label: 'Sẵn sàng' },
    { value: 'ALL', label: 'Tất cả' },
  ];

  ngOnInit(): void {
    this.reload();

    interval(20_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload(false));
  }

  setFilter(filter: StatusFilter): void {
    this.activeFilter.set(filter);
    this.page.set(0);
    this.reload();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.reload();
  }

  needsStartAction(order: Order): boolean {
    return enumEquals(order.status, 'CONFIRMED');
  }

  needsReadyAction(order: Order): boolean {
    return enumEquals(order.status, 'PREPARING');
  }

  startPreparing(order: Order, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.runAction(order.id, () => this.kitchenService.startPreparing(order.id));
  }

  markReady(order: Order, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.runAction(order.id, () => this.kitchenService.markReady(order.id));
  }

  private runAction(orderId: string, action: () => ReturnType<KitchenService['startPreparing']>): void {
    this.actionOrderId.set(orderId);
    this.errorMessage.set(null);

    action().subscribe({
      next: () => {
        this.actionOrderId.set(null);
        this.reload();
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
    this.kitchenService
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
