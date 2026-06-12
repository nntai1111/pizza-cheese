//kiểm tra đã đăng nhập chưa

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
// CanActivateFn:Định nghĩa đây là một guard function.
import { AuthService } from '../services/auth.service';


// nếu đã đăng nhập thì vào app, chưa đăng nhập thì vào login
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
// nếu đã login thì vào dashboard
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
