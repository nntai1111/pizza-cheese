import { Routes } from '@angular/router';

import { authGuard } from '../core/guards/auth.guard';
import { AUTH_ROUTES } from '../features/auth/auth.routes';
import { ADMIN_ROUTES } from '../features/admin/admin.routes';
import { CUSTOMER_ROUTES } from '../features/customer/customer.routes';
import { CASHIER_ROUTES } from '../features/cashier/cashier.routes';
import { KITCHEN_ROUTES } from '../features/kitchen/kitchen.routes';
import { DELIVERY_ROUTES } from '../features/delivery/delivery.routes';
import { ROLES_ROUTES } from '../features/roles/roles.routes';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  ...AUTH_ROUTES,
  {
    path: 'dashboard',
    loadComponent: () =>
      import('../features/dashboard/role-select/role-select.component').then(
        (m) => m.RoleSelectComponent,
      ),
    canActivate: [authGuard],
  },
  ...CUSTOMER_ROUTES,
  ...ADMIN_ROUTES,
  ...CASHIER_ROUTES,
  ...KITCHEN_ROUTES,
  ...DELIVERY_ROUTES,
  ...ROLES_ROUTES,
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
