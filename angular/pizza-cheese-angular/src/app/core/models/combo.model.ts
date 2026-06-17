import { PizzaSize } from './pizza.model';

export interface ComboItem {
  pizzaId: string;
  pizzaVariantId: string;
  pizzaSize: PizzaSize;
  pizzaName: string;
  pizzaSlug: string;
  quantity: number;
}

export interface Combo {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  price: number;
  discountPercent: number | null;
  imageUrl: string | null;
  active: boolean;
  isActive?: boolean;
  items: ComboItem[];
  createdAt: string;
  updatedAt: string;
}

export interface ComboItemRequest {
  pizzaId: string;
  pizzaVariantId: string;
  quantity: number;
}

export interface CreateComboRequest {
  name: string;
  slug?: string;
  description?: string;
  price: number;
  discountPercent?: number | null;
  imageUrl?: string;
  isActive?: boolean;
  items: ComboItemRequest[];
}

export interface UpdateComboRequest {
  name?: string;
  slug?: string;
  description?: string;
  price?: number;
  discountPercent?: number | null;
  imageUrl?: string;
  isActive?: boolean;
  items?: ComboItemRequest[];
}
