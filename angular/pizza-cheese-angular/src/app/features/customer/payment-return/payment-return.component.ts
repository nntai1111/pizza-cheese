import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { OrderService } from '../../../core/services/order.service';
import { Order, ORDER_STATUS_LABELS } from '../../../core/models/order.model';
import { formatVnd } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { enumEquals, getEnumLabel } from '../../../core/utils/coded-enum.util';
import { PENDING_ORDER_KEY } from '../checkout/checkout.component';

@Component({
  selector: 'app-payment-return',
  imports: [RouterLink],
  templateUrl: './payment-return.component.html',
  styleUrl: './payment-return.component.scss',
})
export class PaymentReturnComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);

  readonly order = signal<Order | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly responseCode = signal<string | null>(null);

  readonly formatPrice = formatVnd;
  readonly statusLabels = ORDER_STATUS_LABELS;
  readonly getStatusLabel = (order: Order) => getEnumLabel(order.status, ORDER_STATUS_LABELS);

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.responseCode.set(params.get('vnp_ResponseCode'));

    const txnRef = params.get('vnp_TxnRef');
    const pendingOrderId = sessionStorage.getItem(PENDING_ORDER_KEY);

    if (txnRef) {
      this.orderService.getPaymentStatus(txnRef).subscribe({
        next: (order) => {
          this.order.set(order);
          this.loading.set(false);
          sessionStorage.removeItem(PENDING_ORDER_KEY);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tra cứu đơn hàng.'));
          this.loadByOrderId(pendingOrderId);
        },
      });
      return;
    }

    this.loadByOrderId(pendingOrderId);
  }

  get isSuccess(): boolean {
    const order = this.order();
    return this.responseCode() === '00' || enumEquals(order?.paymentStatus, 'PAID');
  }

  private loadByOrderId(orderId: string | null): void {
    if (!orderId) {
      this.loading.set(false);
      this.errorMessage.set('Không tìm thấy thông tin đơn hàng.');
      return;
    }

    this.orderService.getOrder(orderId).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
        sessionStorage.removeItem(PENDING_ORDER_KEY);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tra cứu đơn hàng.'));
      },
    });
  }
}
