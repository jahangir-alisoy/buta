import { useParams } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Globe, MapPin } from 'lucide-react';
import { catalogApi } from '../../shared/api/endpoints';
import { ApiError } from '../../shared/api/client';
import { EmptyState, ErrorState } from '../../design-system/components/States';
import { PageLoader } from '../../app/PageLoader';
import NotFoundPage from '../info/NotFoundPage';
import { OfferGrid } from './OfferCollection';

function safeWebsite(url: string | undefined): string | undefined {
  if (!url) {
    return undefined;
  }
  try {
    const parsed = new URL(url);
    return parsed.protocol === 'https:' || parsed.protocol === 'http:' ? parsed.toString() : undefined;
  } catch {
    return undefined;
  }
}

export default function PartnerPage() {
  const { slug = '' } = useParams();
  const { t } = useTranslation();
  const partner = useQuery({ queryKey: ['partner', slug], queryFn: () => catalogApi.partner(slug) });

  if (partner.isLoading) {
    return <PageLoader />;
  }
  if (partner.error instanceof ApiError && partner.error.status === 404) {
    return <NotFoundPage />;
  }
  if (partner.isError || !partner.data) {
    return (
      <div className="container page">
        <ErrorState error={partner.error} onRetry={() => partner.refetch()} />
      </div>
    );
  }
  const data = partner.data;
  const website = safeWebsite(data.websiteUrl);
  return (
    <div className="container page">
      <header className="page-header">
        <div className="row" style={{ gap: 16, alignItems: 'flex-start' }}>
          {data.logoPath && <img className="partner-logo" src={data.logoPath} alt="" />}
          <div className="stack" style={{ gap: 6 }}>
            <h1 className="page-title">{data.name}</h1>
            <p className="muted">{data.description}</p>
            <div className="row wrap small muted">
              <span className="row" style={{ gap: 6 }}>
                <MapPin size={16} aria-hidden="true" />
                {data.address ? `${data.address}, ${data.city}` : data.city}
              </span>
              {website && (
                <a className="row section-link" style={{ gap: 6 }} href={website} target="_blank" rel="noopener noreferrer">
                  <Globe size={16} aria-hidden="true" />
                  {t('partners.website')}
                </a>
              )}
            </div>
          </div>
        </div>
      </header>
      {data.offers.length === 0 ? <EmptyState title={t('partners.noOffers')} /> : <OfferGrid offers={data.offers} />}
    </div>
  );
}
