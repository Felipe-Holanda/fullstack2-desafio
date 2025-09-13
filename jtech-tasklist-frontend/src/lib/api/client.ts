const BASE_URL = 'http://localhost:8080';

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export interface ApiErrorData {
  status: number;
  message?: string;
  error?: string;
  path?: string;
  timestamp?: string;
  errors?: Array<{ field?: string; message?: string }>;
}

export class ApiError extends Error {
  status: number;
  data?: unknown;
  constructor(status: number, message: string, data?: unknown) {
    super(message);
    this.status = status;
    this.data = data;
  }
}

export interface RequestOptions<TBody = unknown> {
  method?: HttpMethod;
  path: string;
  query?: Record<string, string | number | boolean | undefined | null>;
  body?: TBody;
  headers?: Record<string, string>;
  auth?: boolean;
}

function buildQuery(query?: RequestOptions['query']) {
  if (!query) return '';
  const params = new URLSearchParams();
  Object.entries(query).forEach(([k, v]) => {
    if (v !== undefined && v !== null) params.append(k, String(v));
  });
  const s = params.toString();
  return s ? `?${s}` : '';
}

// Use centralized token storage to avoid key mismatches
import { getToken as readStoredToken } from '@/lib/auth/storage';

export async function apiFetch<TResp = unknown, TBody = unknown>(opts: RequestOptions<TBody>): Promise<TResp> {
  const {
    method = 'GET',
    path,
    query,
    body,
    headers = {},
    auth = true,
  } = opts;

  const url = `${BASE_URL}${path}${buildQuery(query)}`;

  const finalHeaders: Record<string, string> = {
    'Accept': 'application/json',
    ...headers,
  };

  if (body !== undefined && body !== null && method !== 'GET') {
    finalHeaders['Content-Type'] = 'application/json';
  }

  if (auth) {
    const token = readStoredToken();
    if (token) {
      finalHeaders['Authorization'] = `Bearer ${token}`;
    }
  }

  const res = await fetch(url, {
    method,
    headers: finalHeaders,
    body: body !== undefined && body !== null && method !== 'GET' ? JSON.stringify(body) : undefined,
    credentials: 'omit',
  });

  const contentType = res.headers.get('content-type') || '';
  const isJson = contentType.includes('application/json');
  const data = isJson ? await res.json().catch(() => undefined) : await res.text().catch(() => undefined);

  if (!res.ok) {
    const message = (data && (data.message || data.error)) || res.statusText || 'Request failed';
    throw new ApiError(res.status, message, data);
  }

  return data as TResp;
}

export const http = {
  get: <T>(path: string, query?: RequestOptions['query'], auth = true) =>
    apiFetch<T>({ method: 'GET', path, query, auth }),
  post: <T, B = unknown>(path: string, body?: B, auth = true) =>
    apiFetch<T, B>({ method: 'POST', path, body, auth }),
  put: <T, B = unknown>(path: string, body?: B, auth = true) =>
    apiFetch<T, B>({ method: 'PUT', path, body, auth }),
  patch: <T, B = unknown>(path: string, body?: B, auth = true, query?: RequestOptions['query']) =>
    apiFetch<T, B>({ method: 'PATCH', path, body, auth, query }),
  del: <T>(path: string, auth = true) =>
    apiFetch<T>({ method: 'DELETE', path, auth }),
};

export default http;
