import { ApiEnumField } from './coded-enum.model';
import { PizzaSize } from './pizza.model';

export type LineItemType = 'PIZZA' | 'COMBO';

export interface CartItemTopping {
  toppingId: string;
  toppingName: string;
  price: number;
}

export interface CartItemComboLine {
  pizzaId: string;
  pizzaVariantId: string;
  quantity: number;
  pizzaName: string;
  pizzaSize: ApiEnumField<PizzaSize>;
}

export interface CartItem {
  id: string;
  itemType: ApiEnumField<LineItemType>;
  pizzaId: string | null;
  pizzaVariantId: string | null;
  comboId: string | null;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  pizzaName: string | null;
  pizzaSlug: string | null;
  pizzaSize: ApiEnumField<PizzaSize> | null;
  pizzaImageUrl: string | null;
  comboName: string | null;
  comboSlug: string | null;
  comboImageUrl: string | null;
  toppings: CartItemTopping[];
  comboLines: CartItemComboLine[];
}

export interface Cart {
  id: string | null;
  items: CartItem[];
  itemCount: number;
  subtotal: number;
}

export interface AddPizzaToCartRequest {
  pizzaId: string;
  pizzaVariantId: string;
  toppingIds: string[];
  quantity: number;
}

export interface AddComboToCartRequest {
  comboId: string;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}
