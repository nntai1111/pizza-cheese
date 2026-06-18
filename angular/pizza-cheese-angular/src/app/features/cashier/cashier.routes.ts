import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { AppRole } from '../../core/enums/role.enum';

export const CASHIER_ROUTES: Routes = [
  {
    path: 'cashier',
    loadComponent: () =>
      import('./cashier-layout/cashier-layout.component').then(
        (m) => m.CashierLayoutComponent,
      ),
    canActivate: [authGuard, roleGuard(AppRole.CASHIER)],
    children: [
      { path: '', redirectTo: 'pizzas', pathMatch: 'full' },
      {
        path: 'pizzas',
        loadComponent: () =>
          import('../customer/pizza-list/pizza-list.component').then(
            (m) => m.PizzaListComponent,
          ),
      },
      {
        path: 'pizzas/:id',
        loadComponent: () =>
          import('../customer/pizza-detail/pizza-detail.component').then(
            (m) => m.PizzaDetailComponent,
          ),
      },
      {
        path: 'combos',
        loadComponent: () =>
          import('../customer/combo-list/combo-list.component').then(
            (m) => m.ComboListComponent,
          ),
      },
      {
        path: 'combos/:id',
        loadComponent: () =>
          import('../customer/combo-detail/combo-detail.component').then(
            (m) => m.ComboDetailComponent,
          ),
      },
      {
        path: 'cart',
        loadComponent: () =>
          import('../customer/cart/cart.component').then((m) => m.CartComponent),
      },
      {
        path: 'checkout',
        loadComponent: () =>
          import('../customer/checkout/checkout.component').then(
            (m) => m.CheckoutComponent,
          ),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./order-list/cashier-order-list.component').then(
            (m) => m.CashierOrderListComponent,
          ),
      },
      {
        path: 'orders/:id',
        loadComponent: () =>
          import('./order-detail/cashier-order-detail.component').then(
            (m) => m.CashierOrderDetailComponent,
          ),
      },
    ],
  },
];
