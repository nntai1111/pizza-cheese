import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { AppRole } from '../../core/enums/role.enum';

export const DELIVERY_ROUTES: Routes = [
  {
    path: 'delivery',
    loadComponent: () =>
      import('./delivery-layout/delivery-layout.component').then(
        (m) => m.DeliveryLayoutComponent,
      ),
    canActivate: [authGuard, roleGuard(AppRole.DELIVERY)],
    children: [
      { path: '', redirectTo: 'orders', pathMatch: 'full' },
      {
        path: 'orders',
        loadComponent: () =>
          import('./order-list/delivery-order-list.component').then(
            (m) => m.DeliveryOrderListComponent,
          ),
      },
      {
        path: 'orders/:id',
        loadComponent: () =>
          import('./order-detail/delivery-order-detail.component').then(
            (m) => m.DeliveryOrderDetailComponent,
          ),
      },
    ],
  },
];
