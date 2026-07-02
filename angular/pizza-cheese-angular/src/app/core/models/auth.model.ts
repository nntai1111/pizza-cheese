import { AppRole } from '../enums/role.enum';
import { CodedEnumValue } from './coded-enum.model';

export interface ApiResponse<T> {
  statusCode: number;
  message: string;
  error: string | null;
  data: T;
}

export interface LoginRequest {
  login: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
  phone: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface User {
  id: string;
  username: string;
  email: string;
  name: string;
  phone: string | null;
  avatarUrl: string | null;
  roles: CodedEnumValue<AppRole>[];
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
