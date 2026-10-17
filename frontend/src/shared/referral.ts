const KEY = 'buta.referral';

export function rememberReferral(code: string) {
  const cleaned = code.trim().toUpperCase();
  if (!/^[A-Z0-9]{4,16}$/.test(cleaned)) {
    return;
  }
  try {
    sessionStorage.setItem(KEY, cleaned);
  } catch {
    return;
  }
}

export function rememberedReferral(): string {
  try {
    return sessionStorage.getItem(KEY) ?? '';
  } catch {
    return '';
  }
}
