import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';

export const ROLES_ROUTES: Routes = [
  {
    path: 'welcome/:role',
    loadComponent: () =>
      import('./welcome/welcome.component').then((m) => m.WelcomeComponent),
    canActivate: [authGuard],
  },
];
