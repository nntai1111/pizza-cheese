import { Component, inject, OnInit, signal } from '@angular/core';
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
import { UserAvatarComponent } from '../../../shared/components';

@Component({
  selector: 'app-customer-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, UserAvatarComponent],
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
  readonly isCartPage = signal(false);

  readonly formatPrice = formatVnd;
  readonly getItemTitle = getCartItemTitle;
  readonly getItemImage = getCartItemImage;
  readonly isComboItem = (item: CartItem) => enumEquals(item.itemType, 'COMBO');

  ngOnInit(): void {
    this.shopContext.setBasePath('/customer');
    this.isCartPage.set(this.shopContext.isCartPage(this.router.url));
    this.cartService.loadCart().subscribe();

    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.isCartPage.set(this.shopContext.isCartPage(this.router.url));
      });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
