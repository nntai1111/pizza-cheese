import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { User } from '../../../core/models/auth.model';
import {
  Order,
  ORDER_STATUS_LABELS,
  PAYMENT_METHOD_LABELS,
  PAYMENT_STATUS_LABELS,
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
import { UserAvatarComponent } from '../../../shared/components';

@Component({
  selector: 'app-admin-customer-detail',
  imports: [DatePipe, RouterLink, PaginationComponent, UserAvatarComponent],
  templateUrl: './admin-customer-detail.component.html',
  styleUrl: './admin-customer-detail.component.scss',
})
export class AdminCustomerDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly adminService = inject(AdminService);

  readonly customer = signal<User | null>(null);
  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly ordersLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
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

  private customerId = '';

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loading.set(false);
      this.ordersLoading.set(false);
      this.errorMessage.set('Không tìm thấy khách hàng.');
      return;
    }
    this.customerId = id;
    this.loadCustomer();
    this.loadOrders(0);
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.loadOrders(page);
  }

  private loadCustomer(): void {
    this.loading.set(true);
    this.adminService.getCustomer(this.customerId).subscribe({
      next: (customer) => {
        this.customer.set(customer);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tải khách hàng.'));
      },
    });
  }

  private loadOrders(page: number): void {
    this.ordersLoading.set(true);
    this.adminService.getCustomerOrders(this.customerId, { page, size: this.pageSize }).subscribe({
      next: (result) => {
        this.orders.set(result.content);
        this.totalPages.set(result.totalPages);
        this.totalElements.set(result.totalElements);
        this.ordersLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.ordersLoading.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tải lịch sử đơn.'));
      },
    });
  }
}
