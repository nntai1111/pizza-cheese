import { Routes } from '@angular/router';

import { guestGuard } from '../../core/guards/auth.guard';

export const AUTH_ROUTES: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./login/login.component').then((m) => m.LoginComponent),
    canActivate: [guestGuard],
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./register/register.component').then((m) => m.RegisterComponent),
    canActivate: [guestGuard],
  },
  {
    path: 'check-email',
    loadComponent: () =>
      import('./check-email/check-email.component').then(
        (m) => m.CheckEmailComponent,
      ),
    canActivate: [guestGuard],
  },
  {
    path: 'verify-email',
    loadComponent: () =>
      import('./verify-email/verify-email.component').then(
        (m) => m.VerifyEmailComponent,
      ),
    canActivate: [guestGuard],
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./forgot-password/forgot-password.component').then(
        (m) => m.ForgotPasswordComponent,
      ),
    canActivate: [guestGuard],
  },
  {
    path: 'verify-reset-otp',
    loadComponent: () =>
      import('./verify-reset-otp/verify-reset-otp.component').then(
        (m) => m.VerifyResetOtpComponent,
      ),
    canActivate: [guestGuard],
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./reset-password/reset-password.component').then(
        (m) => m.ResetPasswordComponent,
      ),
    canActivate: [guestGuard],
  },
];
