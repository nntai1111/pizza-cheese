import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { Payment } from '../../../core/models/payment.model';
import {
  PAYMENT_METHOD_LABELS,
  PAYMENT_STATUS_LABELS,
} from '../../../core/models/order.model';
import { formatVnd } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { getEnumLabel } from '../../../core/utils/coded-enum.util';
import { paymentStatusTone, statusBadgeClass } from '../../../core/utils/status-tone.util';

@Component({
  selector: 'app-admin-payment-detail',
  imports: [RouterLink, DatePipe],
  templateUrl: './admin-payment-detail.component.html',
  styleUrl: './admin-payment-detail.component.scss',
})
export class AdminPaymentDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly adminService = inject(AdminService);

  readonly payment = signal<Payment | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly formatPrice = formatVnd;
  readonly getMethodLabel = (p: Payment) => getEnumLabel(p.paymentMethod, PAYMENT_METHOD_LABELS);
  readonly getStatusLabel = (p: Payment) => getEnumLabel(p.status, PAYMENT_STATUS_LABELS);
  readonly paymentBadgeClass = (p: Payment) =>
    `${statusBadgeClass(paymentStatusTone(p.status))} status-badge--lg`;

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loading.set(false);
      this.errorMessage.set('Không tìm thấy thanh toán.');
      return;
    }

    this.adminService.getPayment(id).subscribe({
      next: (payment) => {
        this.payment.set(payment);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tải thanh toán.'));
      },
    });
  }
}
