// gắn bearer token vào header của request

import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { AUTH_ENDPOINTS } from '../constants/api.constants';
import { AuthService } from '../services/auth.service';

const AUTH_URLS = [
  AUTH_ENDPOINTS.login,
  AUTH_ENDPOINTS.register,
  AUTH_ENDPOINTS.logout,
  AUTH_ENDPOINTS.refresh,
];


//HttpInterceptorFn đánh dấu là 1 hàm interceptor
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  //Không gắn token vào những API này.login,refresh token,register
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
  // Request đi backend.
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // nếu lỗi không phải 401 hoặc là request login/refresh thì trả về lỗi luôn
      if (
        error.status !== 401 ||
        isAuthRequest ||
        req.url.includes('/auth/refresh')
      ) {
        return throwError(() => error);
      }
      // nếu lỗi 401 và là request bình thường, thì kiểm tra refresh token còn hạn không
      // nếu refresh token hết hạn thì logout và chuyển về login
      if (!authService.isRefreshTokenValid()) {
        authService.logout();
        router.navigate(['/login']);
        return throwError(() => error);
      }
      // nếu refresh token còn hạn thì gọi API refresh token để lấy access token mới
      //  rồi retry request cũ với access token mới
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
