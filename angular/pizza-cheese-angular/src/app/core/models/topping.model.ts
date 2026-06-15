export interface Topping {
  id: string;
  name: string;
  price: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateToppingRequest {
  name: string;
  price: number;
  isActive?: boolean;
}

export interface UpdateToppingRequest {
  name?: string;
  price?: number;
  isActive?: boolean;
}
