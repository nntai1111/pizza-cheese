import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { AppRole } from '../../core/enums/role.enum';

export const CUSTOMER_ROUTES: Routes = [
  {
    path: 'customer/pizzas',
    loadComponent: () =>
      import('./pizza-list/pizza-list.component').then(
        (m) => m.PizzaListComponent,
      ),
    canActivate: [authGuard, roleGuard(AppRole.CUSTOMER)],
  },
];
