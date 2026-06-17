import { Pizza, PizzaImage, PizzaSize, PizzaVariant } from '../models/pizza.model';

const SIZE_ORDER: PizzaSize[] = ['SMALL', 'MEDIUM', 'LARGE'];

const SIZE_LABELS: Record<PizzaSize, string> = {
  SMALL: 'Nhỏ (25cm)',
  MEDIUM: 'Vừa (30cm)',
  LARGE: 'Lớn (35cm)',
};

function isMainImage(image: PizzaImage): boolean {
  return Boolean(image.main || image.isMain);
}

export function getPizzaMainImage(pizza: Pizza): string | null {
  if (!pizza.images?.length) {
    return null;
  }

  const main =
    pizza.images.find((img) => isMainImage(img)) ?? pizza.images[0];
  return main.imageUrl ?? null;
}

export function getPizzaSecondaryImages(pizza: Pizza): PizzaImage[] {
  if (!pizza.images?.length) {
    return [];
  }

  return pizza.images.filter((img) => !isMainImage(img));
}

export function getPizzaSortedImages(pizza: Pizza): PizzaImage[] {
  if (!pizza.images?.length) {
    return [];
  }

  return [...pizza.images].sort((a, b) => {
    const mainDiff = Number(isMainImage(b)) - Number(isMainImage(a));
    if (mainDiff !== 0) {
      return mainDiff;
    }
    return (a.sortOrder ?? 0) - (b.sortOrder ?? 0);
  });
}

export function sortPizzaVariants(variants: PizzaVariant[]): PizzaVariant[] {
  return [...variants].sort(
    (a, b) => SIZE_ORDER.indexOf(a.size) - SIZE_ORDER.indexOf(b.size),
  );
}

export function getPizzaSizeLabel(size: PizzaSize): string {
  return SIZE_LABELS[size];
}

export function getPizzaMinPrice(pizza: Pizza): number {
  if (pizza.variants.length) {
    return Math.min(...pizza.variants.map((v) => v.price));
  }
  return pizza.basePrice;
}

export function formatVnd(price: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(price);
}
