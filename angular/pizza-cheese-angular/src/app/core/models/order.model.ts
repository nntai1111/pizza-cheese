import { PizzaSize } from './pizza.model';

export type PaymentMethod = 'COD' | 'VNPAY' | 'MOMO' | 'STRIPE';

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED';

export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';

export interface DeliveryAddress {
  recipientName: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string;
  ward?: string;
  district?: string;
  city: string;
}

export interface CreateOrderRequest {
  paymentMethod: PaymentMethod;
  deliveryAddress: DeliveryAddress;
  note?: string;
  cartItemIds: string[];
}

export interface OrderItemTopping {
  toppingId: string;
  toppingName: string;
  price: number;
}

export interface OrderItemComboLine {
  quantity: number;
  pizzaName: string;
  pizzaSize: PizzaSize;
}

export interface OrderItem {
  id: string;
  itemType: 'PIZZA' | 'COMBO';
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  pizzaName: string | null;
  pizzaSize: PizzaSize | null;
  pizzaImageUrl: string | null;
  comboName: string | null;
  comboImageUrl: string | null;
  toppings: OrderItemTopping[];
  comboLines: OrderItemComboLine[];
}

export interface Order {
  id: string;
  orderCode: string;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus | null;
  totalAmount: number;
  discountAmount: number;
  finalAmount: number;
  note: string | null;
  deliveryAddressSnapshot: string;
  paymentUrl: string | null;
  paymentTxnRef: string | null;
  createdAt: string;
  paidAt: string | null;
  items: OrderItem[] | null;
}

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  CONFIRMED: 'Đã xác nhận',
  PREPARING: 'Đang chế biến',
  READY: 'Sẵn sàng',
  OUT_FOR_DELIVERY: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
  REFUNDED: 'Đã hoàn tiền',
};

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  COD: 'Thanh toán khi nhận hàng (COD)',
  VNPAY: 'VNPay',
  MOMO: 'MoMo',
  STRIPE: 'Stripe',
};
