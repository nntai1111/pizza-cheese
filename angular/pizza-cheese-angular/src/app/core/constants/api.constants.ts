import { environment } from '../../../environments/environment';

export const API_BASE_URL = environment.apiUrl;

export const AUTH_ENDPOINTS = {
  login: `${API_BASE_URL}/auth/login`,
  refresh: `${API_BASE_URL}/auth/refresh`,
} as const;

export const STORAGE_KEYS = {
  accessToken: 'pc_access_token',
  refreshToken: 'pc_refresh_token',
  tokenType: 'pc_token_type',
  expiresAt: 'pc_expires_at',
  refreshExpiresAt: 'pc_refresh_expires_at',
  user: 'pc_user',
} as const;
