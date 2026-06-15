import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import {
  Observable,
  catchError,
  finalize,
  map,
  shareReplay,
  throwError,
} from 'rxjs';

import {
  AUTH_ENDPOINTS,
  DEFAULT_AVATAR_URL,
  STORAGE_KEYS,
} from '../constants/api.constants';
import {
  ApiResponse,
  AuthData,
  LoginRequest,
  RefreshTokenRequest,
  RegisterRequest,
  User,
} from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly currentUser = signal<User | null>(this.loadUser());
  private refreshInProgress$: Observable<AuthData> | null = null;

  login(credentials: LoginRequest): Observable<AuthData> {
    return this.http
      .post<ApiResponse<AuthData>>(AUTH_ENDPOINTS.login, credentials)
      .pipe(map((response) => this.persistAuth(response.data)));
  }

  register(payload: RegisterRequest, avatarFile?: File | null): Observable<AuthData> {
    if (avatarFile) {
      const formData = new FormData();
      formData.append(
        'request',
        new Blob([JSON.stringify(payload)], { type: 'application/json' }),
      );
      formData.append('avatar', avatarFile, avatarFile.name);

      return this.http
        .post<ApiResponse<AuthData>>(AUTH_ENDPOINTS.register, formData)
        .pipe(map((response) => this.persistAuth(response.data)));
    }

    return this.http
      .post<ApiResponse<AuthData>>(AUTH_ENDPOINTS.register, payload)
      .pipe(map((response) => this.persistAuth(response.data)));
  }

  refreshAccessToken(): Observable<AuthData> {
    if (this.refreshInProgress$) {
      return this.refreshInProgress$;
    }

    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    const body: RefreshTokenRequest = { refreshToken };

    this.refreshInProgress$ = this.http
      .post<ApiResponse<AuthData>>(AUTH_ENDPOINTS.refresh, body)
      .pipe(
        map((response) => this.persistAuth(response.data)),
        catchError((error) => {
          this.clearAuth();
          return throwError(() => error);
        }),
        finalize(() => {
          this.refreshInProgress$ = null;
        }),
        shareReplay(1),
      );

    return this.refreshInProgress$;
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    this.clearAuth();

    if (refreshToken) {
      const body: RefreshTokenRequest = { refreshToken };
      this.http
        .post<ApiResponse<null>>(AUTH_ENDPOINTS.logout, body)
        .subscribe();
    }
  }

  getAvatarUrl(user: User | null | undefined): string {
    return user?.avatarUrl?.trim() || DEFAULT_AVATAR_URL;
  }

  isAuthenticated(): boolean {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();

    if (!accessToken && !refreshToken) {
      return false;
    }

    if (this.isAccessTokenValid()) {
      return true;
    }

    return this.isRefreshTokenValid();
  }

  isAccessTokenValid(): boolean {
    const token = this.getAccessToken();
    const expiresAt = localStorage.getItem(STORAGE_KEYS.expiresAt);

    if (!token || !expiresAt) {
      return false;
    }

    return new Date(expiresAt).getTime() > Date.now();
  }

  isRefreshTokenValid(): boolean {
    const token = this.getRefreshToken();
    const expiresAt = localStorage.getItem(STORAGE_KEYS.refreshExpiresAt);

    if (!token || !expiresAt) {
      return false;
    }

    return new Date(expiresAt).getTime() > Date.now();
  }

  getAccessToken(): string | null {
    return localStorage.getItem(STORAGE_KEYS.accessToken);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(STORAGE_KEYS.refreshToken);
  }

  getTokenType(): string {
    return localStorage.getItem(STORAGE_KEYS.tokenType) ?? 'Bearer';
  }

  private persistAuth(data: AuthData): AuthData {
    localStorage.setItem(STORAGE_KEYS.accessToken, data.accessToken);
    localStorage.setItem(STORAGE_KEYS.refreshToken, data.refreshToken);
    localStorage.setItem(STORAGE_KEYS.tokenType, data.tokenType);
    localStorage.setItem(STORAGE_KEYS.expiresAt, data.expiresAt);
    localStorage.setItem(STORAGE_KEYS.refreshExpiresAt, data.refreshExpiresAt);
    localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(data.user));
    this.currentUser.set(data.user);
    return data;
  }

  private loadUser(): User | null {
    const raw = localStorage.getItem(STORAGE_KEYS.user);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  }

  private clearAuth(): void {
    Object.values(STORAGE_KEYS).forEach((key) => localStorage.removeItem(key));
    this.currentUser.set(null);
  }
}
