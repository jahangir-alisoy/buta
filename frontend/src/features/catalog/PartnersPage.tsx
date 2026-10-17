import { Link } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { catalogApi } from '../../shared/api/endpoints';
import { ErrorState, Skeleton } from '../../design-system/components/States';

export default function PartnersPage() {
  const { t } = useTranslation();
  const partners = useQuery({ queryKey: ['partners'], queryFn: catalogApi.partners });
  return (
    <div className="container page">
      <header className="page-header">
        <h1 className="page-title">{t('partners.title')}</h1>
        <p className="muted">{t('partners.subtitle')}</p>
      </header>
      {partners.isLoading && (
        <div className="partner-grid">
          {Array.from({ length: 6 }, (_, index) => (
            <Skeleton key={index} height={120} radius={24} />
          ))}
        </div>
      )}
      {partners.isError && <ErrorState error={partners.error} onRetry={() => partners.refetch()} />}
      {partners.data && (
        <div className="partner-grid">
          {partners.data.map((partner) => (
            <Link key={partner.id} to={`/partners/${partner.slug}`} className="partner-card">
              {partner.logoPath ? <img className="partner-logo" src={partner.logoPath} alt="" loading="lazy" /> : <span className="partner-logo" />}
              <div className="stack" style={{ gap: 4, minWidth: 0 }}>
                <strong>{partner.name}</strong>
                <span className="caption tertiary">
                  {partner.city} · {t('partners.offers', { count: partner.offerCount })}
                </span>
                <p className="small muted offer-subtitle">{partner.description}</p>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
