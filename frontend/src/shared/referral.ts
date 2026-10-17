const KEY = 'buta.referral';

function normalize(code: string): string | null {
  const cleaned = code.trim().toUpperCase();
  return /^[A-Z0-9]{4,16}$/.test(cleaned) ? cleaned : null;
}

export function rememberReferral(code: string) {
  const cleaned = normalize(code);
  if (!cleaned) {
    return;
  }
  try {
    sessionStorage.setItem(KEY, cleaned);
  } catch {
    return;
  }
}

export function rememberedReferral(fromLink?: string | null): string {
  const linked = fromLink ? normalize(fromLink) : null;
  if (linked) {
    return linked;
  }
  try {
    return sessionStorage.getItem(KEY) ?? '';
  } catch {
    return '';
  }
}
