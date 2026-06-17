import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { AppRole } from '../../core/enums/role.enum';

export const CUSTOMER_ROUTES: Routes = [
  {
    path: 'customer',
    loadComponent: () =>
      import('./customer-layout/customer-layout.component').then(
        (m) => m.CustomerLayoutComponent,
      ),
    canActivate: [authGuard, roleGuard(AppRole.CUSTOMER)],
    children: [
      {
        path: '',
        redirectTo: 'pizzas',
        pathMatch: 'full',
      },
      {
        path: 'pizzas',
        loadComponent: () =>
          import('./pizza-list/pizza-list.component').then(
            (m) => m.PizzaListComponent,
          ),
      },
      {
        path: 'pizzas/:id',
        loadComponent: () =>
          import('./pizza-detail/pizza-detail.component').then(
            (m) => m.PizzaDetailComponent,
          ),
      },
      {
        path: 'combos',
        loadComponent: () =>
          import('./combo-list/combo-list.component').then(
            (m) => m.ComboListComponent,
          ),
      },
      {
        path: 'combos/:id',
        loadComponent: () =>
          import('./combo-detail/combo-detail.component').then(
            (m) => m.ComboDetailComponent,
          ),
      },
    ],
  },
];
