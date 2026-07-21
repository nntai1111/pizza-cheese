import { PaymentMethod, PaymentStatus } from './order.model';
import { ApiEnumField } from './coded-enum.model';

export interface Payment {
  id: string;
  orderId: string;
  orderCode: string;
  paymentMethod: ApiEnumField<PaymentMethod>;
  amount: number;
  transactionId: string | null;
  status: ApiEnumField<PaymentStatus>;
  paidAt: string | null;
  createdAt: string;
  updatedAt: string;
}
