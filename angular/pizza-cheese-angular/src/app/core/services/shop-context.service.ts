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

  /** Hide floating cart on flows where it distracts (orders, checkout, cart itself). */
  hideCartSidebar(url: string): boolean {
    const base = this.basePath();
    return (
      url.includes(`${base}/cart`) ||
      url.includes(`${base}/orders`) ||
      url.includes(`${base}/checkout`) ||
      url.includes(`${base}/payment`)
    );
  }
}
