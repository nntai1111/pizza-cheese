import { Category } from '../models/category.model';

export function isCategoryActive(category: Category): boolean {
  return category.active ?? category.isActive ?? false;
}

export function getCategoryImageUrl(category: Category): string | null {
  const url = category.imageUrl?.trim();
  return url || null;
}
