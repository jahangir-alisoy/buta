import i18n from './i18n';
import type { Money } from './api/types';

const TIME_ZONE = 'Asia/Baku';

function locale(): string {
  return i18n.language === 'en' ? 'en-GB' : 'az-AZ';
}

export function formatMoney(money: Money | string | number | undefined | null): string {
  if (money === undefined || money === null) {
    return '';
  }
  const amount = typeof money === 'object' ? Number(money.amount) : Number(money);
  return new Intl.NumberFormat(locale(), {
    style: 'currency',
    currency: 'AZN',
    currencyDisplay: 'narrowSymbol',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

export function formatNumber(value: number): string {
  return new Intl.NumberFormat(locale()).format(value);
}

export function formatDate(value: string | undefined | null): string {
  if (!value) {
    return '';
  }
  return new Intl.DateTimeFormat(locale(), { day: 'numeric', month: 'long', year: 'numeric', timeZone: TIME_ZONE }).format(new Date(value));
}

export function formatDateTime(value: string | undefined | null): string {
  if (!value) {
    return '';
  }
  return new Intl.DateTimeFormat(locale(), {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: TIME_ZONE,
  }).format(new Date(value));
}

export function formatTime(value: string | undefined | null): string {
  if (!value) {
    return '';
  }
  return new Intl.DateTimeFormat(locale(), { hour: '2-digit', minute: '2-digit', timeZone: TIME_ZONE }).format(new Date(value));
}

export function formatPercent(value: string | number): string {
  const number = Number(value);
  return Number.isInteger(number) ? String(number) : number.toFixed(1).replace(/\.0$/, '');
}
