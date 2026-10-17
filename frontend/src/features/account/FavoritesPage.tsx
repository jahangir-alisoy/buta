import { Link } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Heart } from 'lucide-react';
import { accountApi } from '../../shared/api/endpoints';
import { CardGridSkeleton, EmptyState, ErrorState } from '../../design-system/components/States';
import { OfferGrid } from '../catalog/OfferCollection';

export default function FavoritesPage() {
  const { t } = useTranslation();
  const favorites = useQuery({ queryKey: ['favorites'], queryFn: accountApi.favorites, staleTime: 0 });
  return (
    <div className="container page">
      <header className="page-header">
        <h1 className="page-title">{t('nav.favorites')}</h1>
      </header>
      {favorites.isLoading && <CardGridSkeleton count={3} />}
      {favorites.isError && <ErrorState error={favorites.error} onRetry={() => favorites.refetch()} />}
      {favorites.data && favorites.data.length === 0 && (
        <EmptyState
          icon={<Heart size={24} />}
          title={t('nav.favorites')}
          text={t('cart.emptyHint')}
          action={
            <Link to="/explore" className="btn btn-primary">
              {t('cart.browse')}
            </Link>
          }
        />
      )}
      {favorites.data && favorites.data.length > 0 && <OfferGrid offers={favorites.data} />}
    </div>
  );
}
