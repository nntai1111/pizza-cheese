import { environment } from '../../../environments/environment';

export const API_BASE_URL = environment.apiUrl;

export const AUTH_ENDPOINTS = {
  login: `${API_BASE_URL}/auth/login`,
  register: `${API_BASE_URL}/auth/register`,
  logout: `${API_BASE_URL}/auth/logout`,
  refresh: `${API_BASE_URL}/auth/refresh`,
  verifyEmail: `${API_BASE_URL}/auth/verify-email`,
  resendVerification: `${API_BASE_URL}/auth/resend-verification`,
  forgotPassword: `${API_BASE_URL}/auth/forgot-password`,
  verifyResetOtp: `${API_BASE_URL}/auth/verify-reset-otp`,
  resetPassword: `${API_BASE_URL}/auth/reset-password`,
} as const;

export const DEFAULT_AVATAR_URL = '/assets/images/avatar/default-avatar.svg';

export const STORAGE_KEYS = {
  accessToken: 'pc_access_token',
  refreshToken: 'pc_refresh_token',
  tokenType: 'pc_token_type',
  expiresAt: 'pc_expires_at',
  refreshExpiresAt: 'pc_refresh_expires_at',
  user: 'pc_user',
  passwordResetToken: 'pc_password_reset_token',
  passwordResetEmail: 'pc_password_reset_email',
} as const;
