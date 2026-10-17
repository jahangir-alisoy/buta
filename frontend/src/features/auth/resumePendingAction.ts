import type { QueryClient } from '@tanstack/react-query';
import type { TFunction } from 'i18next';
import { accountApi, commerceApi } from '../../shared/api/endpoints';
import { ApiError } from '../../shared/api/client';
import { takePendingAction } from '../../shared/pendingAction';

export async function resumePendingAction(
  queryClient: QueryClient,
  notify: (message: string, kind?: 'success' | 'error' | 'info') => void,
  t: TFunction,
  fallback: string,
): Promise<string> {
  const pending = takePendingAction();
  if (!pending) {
    return fallback;
  }
  try {
    switch (pending.action.type) {
      case 'ADD_TO_CART':
        await commerceApi.addItem(pending.action.offerId, pending.action.quantity, pending.action.recommendationRequestId);
        await queryClient.invalidateQueries({ queryKey: ['cart'] });
        notify(t('auth.resumeAdded'));
        return pending.returnTo;
      case 'ADD_FAVORITE':
        await accountApi.addFavorite(pending.action.offerId);
        await queryClient.invalidateQueries();
        notify(t('auth.resumeFavorite'));
        return pending.returnTo;
      case 'START_CHECKOUT':
        return '/checkout';
    }
  } catch (error) {
    notify(t('auth.resumeUnavailable', { reason: error instanceof ApiError ? error.message : t('errors.generic') }), 'error');
    return pending.returnTo;
  }
  return fallback;
}
