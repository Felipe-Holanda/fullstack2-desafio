import http from '@/lib/api/client';
import type { UserResponse } from '@/lib/api/types';

export const usersService = {
  async getById(id: number) {
    return http.get<UserResponse>(`/api/users/${id}`);
  },
  async me() {
    return http.get<UserResponse>('/api/users/me');
  },
};

export default usersService;
