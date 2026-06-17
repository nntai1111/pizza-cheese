import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { AppRole } from '../../core/enums/role.enum';

export const ADMIN_ROUTES: Routes = [
  {
    path: 'admin',
    loadComponent: () =>
      import('./admin-layout/admin-layout.component').then(
        (m) => m.AdminLayoutComponent,
      ),
    canActivate: [authGuard, roleGuard(AppRole.ADMIN)],
    children: [
      { path: '', redirectTo: 'categories', pathMatch: 'full' },
      {
        path: 'categories',
        loadComponent: () =>
          import('./categories/category-list.component').then(
            (m) => m.CategoryListComponent,
          ),
      },
      {
        path: 'toppings',
        loadComponent: () =>
          import('./toppings/topping-list.component').then(
            (m) => m.ToppingListComponent,
          ),
      },
      {
        path: 'pizzas',
        loadComponent: () =>
          import('./pizzas/pizza-admin-list.component').then(
            (m) => m.PizzaAdminListComponent,
          ),
      },
      {
        path: 'combos',
        loadComponent: () =>
          import('./combos/combo-list.component').then(
            (m) => m.ComboListComponent,
          ),
      },
    ],
  },
];
