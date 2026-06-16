import { Pizza, PizzaImage } from '../models/pizza.model';

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
