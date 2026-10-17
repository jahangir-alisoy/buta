import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Heart } from 'lucide-react';
import { accountApi } from '../../shared/api/endpoints';
import { useAuth } from '../auth/AuthProvider';
import { useToast } from '../../design-system/components/Toast';
import { errorMessage } from '../../design-system/components/States';

export function FavoriteButton({ offerId, favorite }: { offerId: string; favorite: boolean }) {
  const { t } = useTranslation();
  const { requireAuth } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [active, setActive] = useState(favorite);
  const [busy, setBusy] = useState(false);

  useEffect(() => setActive(favorite), [favorite]);

  const toggle = async () => {
    if (!requireAuth({ type: 'ADD_FAVORITE', offerId })) {
      return;
    }
    const next = !active;
    setActive(next);
    setBusy(true);
    try {
      await (next ? accountApi.addFavorite(offerId) : accountApi.removeFavorite(offerId));
      void queryClient.invalidateQueries({ queryKey: ['favorites'] });
    } catch (error) {
      setActive(!next);
      toast.show(errorMessage(error, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <button
      type="button"
      className={`icon-btn${active ? ' is-active' : ''}`}
      onClick={toggle}
      disabled={busy}
      aria-pressed={active}
      aria-label={active ? t('offer.unfavorite') : t('offer.favorite')}
    >
      <Heart size={20} fill={active ? 'currentColor' : 'none'} />
    </button>
  );
}
