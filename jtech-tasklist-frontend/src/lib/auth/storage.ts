// Simple token storage using localStorage with change listeners

type Listener = (token: string | null) => void;
const KEY = 'auth_token';

const listeners = new Set<Listener>();

export function getToken(): string | null {
  try {
    return localStorage.getItem(KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string) {
  try {
    localStorage.setItem(KEY, token);
    listeners.forEach((fn) => fn(token));
  } catch {
    // ignore
  }
}

export function clearToken() {
  try {
    localStorage.removeItem(KEY);
    listeners.forEach((fn) => fn(null));
  } catch {
    // ignore
  }
}

export function onTokenChange(fn: Listener) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}
