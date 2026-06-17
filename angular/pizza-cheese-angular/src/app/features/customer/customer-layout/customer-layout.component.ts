import { Component, inject, OnInit, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import {
  getCartItemImage,
  getCartItemTitle,
} from '../../../core/utils/cart-display.util';
import { formatVnd } from '../../../core/utils/pizza.util';
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
  private readonly router = inject(Router);

  readonly user = this.authService.currentUser;
  readonly cart = this.cartService.cart;
  readonly cartItemCount = this.cartService.itemCount;
  readonly currentYear = new Date().getFullYear();
  readonly isCartPage = signal(this.router.url.includes('/customer/cart'));

  readonly formatPrice = formatVnd;
  readonly getItemTitle = getCartItemTitle;
  readonly getItemImage = getCartItemImage;

  ngOnInit(): void {
    this.cartService.loadCart().subscribe();

    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.isCartPage.set(this.router.url.includes('/customer/cart'));
      });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
