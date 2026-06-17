import { Combo } from '../models/combo.model';
import { formatVnd, getPizzaSizeLabel } from './pizza.util';

export function isComboActive(combo: Combo): boolean {
  return combo.active ?? combo.isActive ?? false;
}

export function getComboImageUrl(combo: Combo): string | null {
  const url = combo.imageUrl?.trim();
  return url || null;
}

export function formatComboPrice(combo: Combo): string {
  return formatVnd(combo.price);
}

export function getComboDiscountedPrice(combo: Combo): number | null {
  if (combo.discountPercent == null || combo.discountPercent <= 0) {
    return null;
  }
  const discounted = combo.price * (1 - combo.discountPercent / 100);
  return Math.round(discounted);
}

export function formatComboItemSummary(combo: Combo): string {
  if (!combo.items?.length) {
    return '';
  }
  return combo.items
    .map((item) => {
      const sizeLabel = item.pizzaSize ? getPizzaSizeLabel(item.pizzaSize) : '';
      return `${item.quantity}x ${item.pizzaName}${sizeLabel ? ` (${sizeLabel})` : ''}`;
    })
    .join(', ');
}
