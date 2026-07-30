//kiểm tra đã đăng nhập chưa

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { getDefaultRouteForUser } from '../utils/role.util';

/** nếu đã đăng nhập thì vào app, chưa đăng nhập thì vào login */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login']);
};

/** nếu đã login thì vào trang làm việc mặc định (theo role), chưa thì vào login/register */
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return true;
  }

  return router.parseUrl(getDefaultRouteForUser(authService.currentUser()));
};

/** `/` và `**`: guest → login, đã login → trang theo role */
export const homeRedirectGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  return router.parseUrl(getDefaultRouteForUser(authService.currentUser()));
};
