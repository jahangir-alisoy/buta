export function safeReturnTo(value: string | null | undefined, fallback = '/'): string {
  if (!value || typeof value !== 'string') {
    return fallback;
  }
  if (!value.startsWith('/') || value.startsWith('//') || value.includes('\\') || /^\/[a-z]+:/i.test(value)) {
    return fallback;
  }
  if (value.startsWith('/auth')) {
    return fallback;
  }
  return value;
}
