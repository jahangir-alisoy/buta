import type { FieldError } from './types';

const ORIGIN = (import.meta.env.VITE_API_URL ?? '').replace(/\/+$/, '');
const BASE = `${ORIGIN}/api`;

let csrfToken: string | null = null;

export function apiUrl(path: string): string {
  return path.startsWith('/api') ? `${ORIGIN}${path}` : path;
}

let currentLanguage = 'az';
let unauthorizedHandler: (() => void) | null = null;

export function setApiLanguage(language: string) {
  currentLanguage = language;
}

export function onUnauthorized(handler: () => void) {
  unauthorizedHandler = handler;
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly fieldErrors: FieldError[] = [],
    readonly requestId?: string,
    readonly properties: Record<string, unknown> = {},
  ) {
    super(message);
  }
}

function readCookie(name: string): string | null {
  const match = document.cookie.split('; ').find((entry) => entry.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.substring(name.length + 1)) : null;
}

function requestId(): string {
  return crypto.randomUUID().replace(/-/g, '').substring(0, 16);
}

export interface RequestOptions {
  method?: string;
  body?: unknown;
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? 'GET';
  const headers: Record<string, string> = {
    Accept: 'application/json',
    'Accept-Language': currentLanguage,
    'X-Request-Id': requestId(),
    ...options.headers,
  };
  let body: BodyInit | undefined;
  if (options.body instanceof FormData) {
    body = options.body;
  } else if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(options.body);
  }
  if (method !== 'GET') {
    const token = csrfToken ?? readCookie('XSRF-TOKEN');
    if (token) {
      headers['X-XSRF-TOKEN'] = token;
    }
  }
  const response = await fetch(`${BASE}/v1${path}`, { method, headers, body, credentials: 'include', signal: options.signal });
  csrfToken = response.headers.get('X-XSRF-TOKEN') ?? csrfToken;
  const text = await response.text();
  const payload = text ? JSON.parse(text) : undefined;
  if (!response.ok) {
    const problem = (payload ?? {}) as Record<string, unknown>;
    const code = typeof problem.code === 'string' ? problem.code : 'INTERNAL_ERROR';
    if (response.status === 401 && unauthorizedHandler) {
      unauthorizedHandler();
    }
    throw new ApiError(
      response.status,
      code,
      typeof problem.detail === 'string' ? problem.detail : code,
      Array.isArray(problem.errors) ? (problem.errors as FieldError[]) : [],
      typeof problem.requestId === 'string' ? problem.requestId : undefined,
      problem,
    );
  }
  return payload as T;
}

export async function ensureCsrfToken(): Promise<void> {
  if (!csrfToken && !readCookie('XSRF-TOKEN')) {
    await api('/auth/session');
  }
}
