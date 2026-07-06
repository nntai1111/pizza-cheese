import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { AppRole } from '../../core/enums/role.enum';

export const KITCHEN_ROUTES: Routes = [
  {
    path: 'kitchen',
    loadComponent: () =>
      import('./kitchen-layout/kitchen-layout.component').then(
        (m) => m.KitchenLayoutComponent,
      ),
    canActivate: [authGuard, roleGuard(AppRole.KITCHEN)],
    children: [
      { path: '', redirectTo: 'orders', pathMatch: 'full' },
      {
        path: 'orders',
        loadComponent: () =>
          import('./order-list/kitchen-order-list.component').then(
            (m) => m.KitchenOrderListComponent,
          ),
      },
      {
        path: 'orders/:id',
        loadComponent: () =>
          import('./order-detail/kitchen-order-detail.component').then(
            (m) => m.KitchenOrderDetailComponent,
          ),
      },
    ],
  },
];
