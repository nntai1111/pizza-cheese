import { ApiEnumField } from './coded-enum.model';

export type PizzaSize = 'SMALL' | 'MEDIUM' | 'LARGE';

export interface CategorySummary {
  id: string;
  name: string;
  slug: string;
}

export interface PizzaVariant {
  id?: string;
  size: ApiEnumField<PizzaSize>;
  price: number;
}

export interface PizzaImage {
  id?: string;
  imageUrl: string;
  main?: boolean;
  isMain?: boolean;
  sortOrder: number;
}

export interface ToppingSummary {
  id: string;
  name: string;
  price: number;
  active: boolean;
}

export interface Pizza {
  id: string;
  category: CategorySummary | null;
  name: string;
  slug: string;
  description: string | null;
  basePrice: number;
  active: boolean;
  variants: PizzaVariant[];
  toppings: ToppingSummary[];
  images: PizzaImage[];
  createdAt: string;
  updatedAt: string;
}

export interface CreatePizzaRequest {
  categoryId: string;
  name: string;
  slug?: string;
  description?: string;
  basePrice: number;
  isActive?: boolean;
  variants: { size: PizzaSize; price: number }[];
  toppingIds?: string[];
}

export interface UpdatePizzaRequest {
  categoryId?: string;
  name?: string;
  slug?: string;
  description?: string;
  basePrice?: number;
  isActive?: boolean;
  variants?: { size: PizzaSize; price: number }[];
  toppingIds?: string[];
}
