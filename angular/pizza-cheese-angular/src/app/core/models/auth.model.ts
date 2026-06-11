export interface ApiResponse<T> {
  statusCode: number;
  message: string;
  error: string | null;
  data: T;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface User {
  id: number;
  email: string;
  name: string;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface AuthData {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  expiresAt: string;
  refreshExpiresIn: number;
  refreshExpiresAt: string;
  user: User;
}
