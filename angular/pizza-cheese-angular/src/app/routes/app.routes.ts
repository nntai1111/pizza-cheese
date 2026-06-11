import { Routes } from '@angular/router';

import { authGuard } from '../core/guards/auth.guard';
import { AUTH_ROUTES } from '../features/auth/auth.routes';
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
  ...ROLES_ROUTES,
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
