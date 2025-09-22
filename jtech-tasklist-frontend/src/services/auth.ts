import http from '@/lib/api/client';
import type { LoginRequest, TokenResponse, UserCreateRequest, UserResponse } from '@/lib/api/types';

export const authService = {
  async login(payload: LoginRequest) {
    return http.post<TokenResponse, LoginRequest>('/api/auth/login', payload, false);
  },

  async register(payload: UserCreateRequest) {
    return http.post<UserResponse, UserCreateRequest>('/api/users/register', payload, false);
  },
};

export default authService;
