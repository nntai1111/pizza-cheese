import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AppRole } from '../enums/role.enum';
import { AuthService } from '../services/auth.service';
import { userHasRole } from '../utils/role.util';

export function roleGuard(requiredRole: AppRole): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const user = authService.currentUser();

    if (!authService.isAuthenticated()) {
      return router.createUrlTree(['/login']);
    }

    if (userHasRole(user, requiredRole)) {
      return true;
    }

    return router.createUrlTree(['/dashboard']);
  };
}
