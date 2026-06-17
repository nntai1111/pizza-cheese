import { CartItem } from '../models/cart.model';
import { getPizzaSizeLabel } from './pizza.util';

export function getCartItemTitle(item: CartItem): string {
  if (item.itemType === 'PIZZA') {
    const size = item.pizzaSize ? ` (${getPizzaSizeLabel(item.pizzaSize)})` : '';
    return `${item.pizzaName ?? 'Pizza'}${size}`;
  }
  return item.comboName ?? 'Combo';
}

export function getCartItemImage(item: CartItem): string | null {
  if (item.itemType === 'PIZZA') {
    return item.pizzaImageUrl;
  }
  return item.comboImageUrl;
}
