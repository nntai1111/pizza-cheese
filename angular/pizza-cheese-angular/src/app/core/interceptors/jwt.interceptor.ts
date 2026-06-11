import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { AUTH_ENDPOINTS } from '../constants/api.constants';
import { AuthService } from '../services/auth.service';

const AUTH_URLS = [AUTH_ENDPOINTS.login, AUTH_ENDPOINTS.refresh];

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuthRequest = AUTH_URLS.some((url) => req.url.includes(url));
  let authReq = req;

  if (!isAuthRequest) {
    const token = authService.getAccessToken();
    if (token) {
      authReq = req.clone({
        setHeaders: {
          Authorization: `${authService.getTokenType()} ${token}`,
        },
      });
    }
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (
        error.status !== 401 ||
        isAuthRequest ||
        req.url.includes('/auth/refresh')
      ) {
        return throwError(() => error);
      }

      if (!authService.isRefreshTokenValid()) {
        authService.logout();
        router.navigate(['/login']);
        return throwError(() => error);
      }

      return authService.refreshAccessToken().pipe(
        switchMap(() => {
          const newToken = authService.getAccessToken();
          const retryReq = req.clone({
            setHeaders: {
              Authorization: `${authService.getTokenType()} ${newToken}`,
            },
          });
          return next(retryReq);
        }),
        catchError((refreshError) => {
          authService.logout();
          router.navigate(['/login']);
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
