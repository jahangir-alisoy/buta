import { useEffect, useMemo } from 'react';
import { useParams } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { catalogApi } from '../../shared/api/endpoints';
import { PageLoader } from '../../app/PageLoader';
import { ErrorState } from '../../design-system/components/States';
import NotFoundPage from '../info/NotFoundPage';
import { OfferBrowser } from './OfferBrowser';
import { track } from './track';

export default function CategoryPage() {
  const { slug = '' } = useParams();
  const categories = useQuery({ queryKey: ['categories'], queryFn: catalogApi.categories });
  const category = categories.data?.find((item) => item.slug === slug);
  const preset = useMemo(() => ({ category: slug }), [slug]);

  useEffect(() => {
    if (category) {
      track({ type: 'CATEGORY_VIEW', categoryId: category.id });
    }
  }, [category]);

  if (categories.isLoading) {
    return <PageLoader />;
  }
  if (categories.isError) {
    return (
      <div className="container page">
        <ErrorState error={categories.error} onRetry={() => categories.refetch()} />
      </div>
    );
  }
  if (!category) {
    return <NotFoundPage />;
  }
  return <OfferBrowser key={slug} title={category.name} preset={preset} />;
}
