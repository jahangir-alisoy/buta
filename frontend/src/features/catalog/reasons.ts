import type { TFunction } from 'i18next';

export function reasonText(t: TFunction, code: string | undefined, params: Record<string, string>): string | undefined {
  if (!code) {
    return undefined;
  }
  return t(`forYou.reasons.${code}`, { ...params, defaultValue: t('forYou.reasons.REASON_FOR_YOU') });
}
