import { safeReturnTo } from './safeReturnTo';

export type PendingAction =
  | { type: 'ADD_TO_CART'; offerId: string; quantity: number; recommendationRequestId?: string }
  | { type: 'ADD_FAVORITE'; offerId: string }
  | { type: 'START_CHECKOUT' };

interface StoredAction {
  action: PendingAction;
  returnTo: string;
  createdAt: number;
}

const KEY = 'buta.pendingAction';
const MAX_AGE_MS = 30 * 60 * 1000;

export function savePendingAction(action: PendingAction, returnTo: string) {
  try {
    sessionStorage.setItem(KEY, JSON.stringify({ action, returnTo: safeReturnTo(returnTo), createdAt: Date.now() } satisfies StoredAction));
  } catch {
    return;
  }
}

export function takePendingAction(): StoredAction | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    sessionStorage.removeItem(KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as StoredAction;
    const valid =
      parsed &&
      typeof parsed.createdAt === 'number' &&
      Date.now() - parsed.createdAt < MAX_AGE_MS &&
      ['ADD_TO_CART', 'ADD_FAVORITE', 'START_CHECKOUT'].includes(parsed.action?.type);
    return valid ? { ...parsed, returnTo: safeReturnTo(parsed.returnTo) } : null;
  } catch {
    return null;
  }
}

export function hasPendingAction(): boolean {
  try {
    return sessionStorage.getItem(KEY) !== null;
  } catch {
    return false;
  }
}
