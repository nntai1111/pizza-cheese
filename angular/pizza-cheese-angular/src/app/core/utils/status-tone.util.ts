import { ApiEnumField } from '../models/coded-enum.model';
import { OrderStatus, PaymentStatus } from '../models/order.model';
import { codedEnumName } from './coded-enum.util';

export type StatusTone =
  | 'neutral'
  | 'info'
  | 'warning'
  | 'success'
  | 'danger'
  | 'purple'
  | 'orange';

const ORDER_STATUS_TONE: Record<OrderStatus, StatusTone> = {
  PENDING_PAYMENT: 'warning',
  CONFIRMED: 'info',
  PREPARING: 'orange',
  READY: 'purple',
  OUT_FOR_DELIVERY: 'info',
  COMPLETED: 'success',
  CANCELLED: 'danger',
  REFUNDED: 'neutral',
};

const PAYMENT_STATUS_TONE: Record<PaymentStatus, StatusTone> = {
  PENDING: 'warning',
  PAID: 'success',
  FAILED: 'danger',
  REFUNDED: 'neutral',
};

export function orderStatusTone(
  status: ApiEnumField<OrderStatus> | null | undefined,
): StatusTone {
  const name = codedEnumName(status);
  return name ? ORDER_STATUS_TONE[name] : 'neutral';
}

export function paymentStatusTone(
  status: ApiEnumField<PaymentStatus> | null | undefined,
): StatusTone {
  const name = codedEnumName(status);
  return name ? PAYMENT_STATUS_TONE[name] : 'neutral';
}

export function statusBadgeClass(tone: StatusTone): string {
  return `status-badge status-badge--${tone}`;
}
