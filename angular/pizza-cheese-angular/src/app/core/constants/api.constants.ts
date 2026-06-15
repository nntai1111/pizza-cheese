import { environment } from '../../../environments/environment';

export const API_BASE_URL = environment.apiUrl;

export const AUTH_ENDPOINTS = {
  login: `${API_BASE_URL}/auth/login`,
  register: `${API_BASE_URL}/auth/register`,
  logout: `${API_BASE_URL}/auth/logout`,
  refresh: `${API_BASE_URL}/auth/refresh`,
} as const;

export const DEFAULT_AVATAR_URL = '/assets/images/avatar/default-avatar.svg';

export const STORAGE_KEYS = {
  accessToken: 'pc_access_token',
  refreshToken: 'pc_refresh_token',
  tokenType: 'pc_token_type',
  expiresAt: 'pc_expires_at',
  refreshExpiresAt: 'pc_refresh_expires_at',
  user: 'pc_user',
} as const;
