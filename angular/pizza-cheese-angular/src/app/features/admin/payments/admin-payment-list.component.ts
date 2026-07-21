import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { Payment } from '../../../core/models/payment.model';
import {
  PAYMENT_METHOD_LABELS,
  PAYMENT_STATUS_LABELS,
  PaymentMethod,
  PaymentStatus,
} from '../../../core/models/order.model';
import { formatVnd } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { getEnumLabel } from '../../../core/utils/coded-enum.util';
import {
  paymentStatusTone,
  statusBadgeClass,
} from '../../../core/utils/status-tone.util';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

type StatusFilter = PaymentStatus | 'ALL';
type MethodFilter = PaymentMethod | 'ALL';

@Component({
  selector: 'app-admin-payment-list',
  imports: [RouterLink, DatePipe, FormsModule, PaginationComponent],
  templateUrl: './admin-payment-list.component.html',
  styleUrl: './admin-payment-list.component.scss',
})
export class AdminPaymentListComponent {
  private readonly adminService = inject(AdminService);

  readonly payments = signal<Payment[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly statusFilter = signal<StatusFilter>('ALL');
  readonly methodFilter = signal<MethodFilter>('ALL');
  readonly fromDate = signal('');
  readonly toDate = signal('');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly pageSize = 10;
  readonly formatPrice = formatVnd;
  readonly getMethodLabel = (p: Payment) => getEnumLabel(p.paymentMethod, PAYMENT_METHOD_LABELS);
  readonly getStatusLabel = (p: Payment) => getEnumLabel(p.status, PAYMENT_STATUS_LABELS);
  readonly paymentBadgeClass = (p: Payment) => statusBadgeClass(paymentStatusTone(p.status));

  readonly statusFilters: { value: StatusFilter; label: string }[] = [
    { value: 'ALL', label: 'Tất cả TT' },
    { value: 'PENDING', label: 'Chờ TT' },
    { value: 'PAID', label: 'Đã TT' },
    { value: 'FAILED', label: 'Thất bại' },
    { value: 'REFUNDED', label: 'Hoàn tiền' },
  ];

  readonly methodFilters: { value: MethodFilter; label: string }[] = [
    { value: 'ALL', label: 'Mọi method' },
    { value: 'COD', label: 'COD' },
    { value: 'VNPAY', label: 'VNPay' },
  ];

  constructor() {
    this.reload();
  }

  setStatus(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    this.page.set(0);
    this.reload();
  }

  setMethod(filter: MethodFilter): void {
    this.methodFilter.set(filter);
    this.page.set(0);
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
    this.loading.set(true);
    this.errorMessage.set(null);

    const status = this.statusFilter();
    const method = this.methodFilter();

    this.adminService
      .getPayments({
        status: status === 'ALL' ? undefined : status,
        method: method === 'ALL' ? undefined : method,
        from: this.fromDate() || undefined,
        to: this.toDate() || undefined,
        page: this.page(),
        size: this.pageSize,
      })
      .subscribe({
        next: (result) => {
          this.payments.set(result.content);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tải danh sách thanh toán.'));
        },
      });
  }
}
