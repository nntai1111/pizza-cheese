import { Injectable, signal } from '@angular/core';

export type ShopBasePath = '/customer' | '/cashier';

@Injectable({ providedIn: 'root' })
export class ShopContextService {
  readonly basePath = signal<ShopBasePath>('/customer');

  setBasePath(path: ShopBasePath): void {
    this.basePath.set(path);
  }

  segments(...parts: string[]): string[] {
    return [this.basePath(), ...parts];
  }

  isCartPage(url: string): boolean {
    return url.includes(`${this.basePath()}/cart`);
  }
}
