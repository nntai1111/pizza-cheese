import { Component, HostListener, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { ShopContextService } from '../../../core/services/shop-context.service';
import {
  getCartItemImage,
  getCartItemTitle,
} from '../../../core/utils/cart-display.util';
import { formatVnd } from '../../../core/utils/pizza.util';
import { enumEquals } from '../../../core/utils/coded-enum.util';
import { CartItem } from '../../../core/models/cart.model';
import { CartItemDetailModalComponent, UserAvatarComponent } from '../../../shared/components';

@Component({
  selector: 'app-customer-layout',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    UserAvatarComponent,
    CartItemDetailModalComponent,
  ],
  templateUrl: './customer-layout.component.html',
  styleUrl: './customer-layout.component.scss',
})
export class CustomerLayoutComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);
  private readonly shopContext = inject(ShopContextService);
  private readonly router = inject(Router);

  readonly user = this.authService.currentUser;
  readonly cart = this.cartService.cart;
  readonly cartItemCount = this.cartService.itemCount;
  readonly shop = this.shopContext;
  readonly currentYear = new Date().getFullYear();
  readonly hideCartSidebar = signal(false);
  readonly cartPreviewOpen = signal(false);
  readonly detailItem = signal<CartItem | null>(null);

  readonly formatPrice = formatVnd;
  readonly getItemTitle = getCartItemTitle;
  readonly getItemImage = getCartItemImage;
  readonly isComboItem = (item: CartItem) => enumEquals(item.itemType, 'COMBO');

  constructor() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.hideCartSidebar.set(this.shopContext.hideCartSidebar(this.router.url));
        this.closeCartPreview();
        this.closeDetail();
      });
  }

  ngOnInit(): void {
    this.shopContext.setBasePath('/customer');
    this.hideCartSidebar.set(this.shopContext.hideCartSidebar(this.router.url));
    this.cartService.loadCart().subscribe();
  }

  toggleCartPreview(): void {
    this.cartPreviewOpen.update((open) => !open);
  }

  closeCartPreview(): void {
    this.cartPreviewOpen.set(false);
  }

  openDetail(item: CartItem): void {
    this.detailItem.set(item);
  }

  closeDetail(): void {
    this.detailItem.set(null);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.detailItem()) {
      this.closeDetail();
      return;
    }
    if (this.cartPreviewOpen()) {
      this.closeCartPreview();
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
