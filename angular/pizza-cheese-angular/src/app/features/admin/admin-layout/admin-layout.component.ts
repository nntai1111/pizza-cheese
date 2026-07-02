import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { UserAvatarComponent } from '../../../shared/components';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, UserAvatarComponent],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
})
export class AdminLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly user = this.authService.currentUser;

  readonly navItems: NavItem[] = [
    { label: 'Danh mục', icon: '📁', route: '/admin/categories' },
    { label: 'Topping', icon: '🧀', route: '/admin/toppings' },
    { label: 'Pizza', icon: '🍕', route: '/admin/pizzas' },
    { label: 'Combo', icon: '🎁', route: '/admin/combos' },
    { label: 'Coupon', icon: '🎟️', route: '/admin/coupons' },
    { label: 'Đơn hàng', icon: '📦', route: '/admin/orders' },
  ];

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
