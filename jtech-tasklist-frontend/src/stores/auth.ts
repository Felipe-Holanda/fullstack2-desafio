import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import authService from '@/services/auth';
import usersService from '@/services/users';
import type { LoginRequest, TokenResponse, UserCreateRequest, UserResponse } from '@/lib/api/types';
import { clearToken, getToken as readToken, setToken } from '@/lib/auth/storage';

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(readToken());
  const currentUser = ref<UserResponse | null>(null);
  const isAuthenticated = computed(() => !!token.value);

  function setAuthToken(t: string | null) {
    token.value = t;
    if (t) setToken(t);
    else clearToken();
  }

  async function login(payload: LoginRequest) {
    const res: TokenResponse = await authService.login(payload);
    setAuthToken(res.token);
    await fetchMe();
    return res;
  }

  async function register(payload: UserCreateRequest) {
    const user = await authService.register(payload);
    // Optionally, auto-login after register by performing a login call
    return user;
  }

  async function fetchMe() {
    if (!token.value) return null;
    const me = await usersService.me();
    currentUser.value = me;
    return me;
  }

  function logout() {
    setAuthToken(null);
    currentUser.value = null;
  }

  return {
    token,
    currentUser,
    isAuthenticated,
    setAuthToken,
    login,
    register,
    fetchMe,
    logout,
  };
});

export default useAuthStore;
