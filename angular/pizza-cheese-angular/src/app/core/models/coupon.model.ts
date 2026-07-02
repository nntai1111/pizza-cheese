import { ApiEnumField } from './coded-enum.model';

export type DiscountType = 'PERCENT' | 'FIXED';

export interface Coupon {
  id: string;
  code: string;
  description: string | null;
  discountType: ApiEnumField<DiscountType>;
  discountValue: number;
  minOrderValue: number | null;
  maxDiscount: number | null;
  startDate: string | null;
  endDate: string | null;
  usageLimit: number | null;
  usedCount: number;
  perUserLimit: number | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCouponRequest {
  code: string;
  description?: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderValue?: number;
  maxDiscount?: number;
  startDate?: string;
  endDate?: string;
  usageLimit?: number;
  perUserLimit?: number;
  isActive?: boolean;
}

export interface UpdateCouponRequest {
  code?: string;
  description?: string;
  discountType?: DiscountType;
  discountValue?: number;
  minOrderValue?: number;
  maxDiscount?: number;
  startDate?: string;
  endDate?: string;
  usageLimit?: number;
  perUserLimit?: number;
  isActive?: boolean;
}

export interface ValidateCouponRequest {
  code: string;
  orderAmount: number;
}

export interface ValidateCouponResponse {
  couponId: string | null;
  code: string | null;
  description: string | null;
  discountType: ApiEnumField<DiscountType> | null;
  orderAmount: number;
  discountAmount: number;
  finalAmount: number;
}

export interface AvailableCoupon {
  id: string;
  code: string;
  description: string | null;
  discountType: ApiEnumField<DiscountType>;
  discountLabel: string;
  minOrderValue: number | null;
  orderAmount: number;
  discountAmount: number;
  finalAmount: number;
}

export const DISCOUNT_TYPE_LABELS: Record<DiscountType, string> = {
  PERCENT: 'Giảm theo %',
  FIXED: 'Giảm cố định',
};
