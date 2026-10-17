import { Link } from 'react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { ApiError } from '../../../shared/api/client';
import { formatMoney } from '../../../shared/format';
import { CardGridSkeleton, ErrorState, errorMessage } from '../../../design-system/components/States';
import { useToast } from '../../../design-system/components/Toast';
import { adminApi } from '../api';
import { AdminHeader, StatusPill, useLocalized } from '../forms';
import type { AdminOffer } from '../types';

export function publishFailureText(error: unknown, t: (key: string, options?: Record<string, unknown>) => string, fallback: string): string {
  if (error instanceof ApiError && Array.isArray(error.properties.failures)) {
    const rules = (error.properties.failures as string[]).map((rule) => t(`admin.offers.rules.${rule}`, { defaultValue: rule })).join(', ');
    return t('admin.offers.publishFailed', { rules });
  }
  return errorMessage(error, fallback);
}

export default function AdminOffersPage() {
  const { t } = useTranslation();
  const toast = useToast();
  const localized = useLocalized();
  const queryClient = useQueryClient();
  const offers = useQuery({ queryKey: ['admin', 'offers'], queryFn: adminApi.offers, staleTime: 0 });

  const change = async (offer: AdminOffer, action: 'publish' | 'pause' | 'archive') => {
    try {
      await adminApi.changeOfferStatus(offer.id, action);
      toast.show(t('admin.saved'));
      void queryClient.invalidateQueries({ queryKey: ['admin', 'offers'] });
      void queryClient.invalidateQueries({ queryKey: ['home'] });
      void queryClient.invalidateQueries({ queryKey: ['offers'] });
    } catch (error) {
      toast.show(publishFailureText(error, t, t('errors.generic')), 'error');
    }
  };

  return (
    <>
      <AdminHeader
        title={t('admin.nav.offers')}
        action={
          <Link to="/admin/offers/new" className="btn btn-primary btn-sm">
            <Plus size={16} aria-hidden="true" />
            {t('admin.new')}
          </Link>
        }
      />
      {offers.isLoading && <CardGridSkeleton count={2} />}
      {offers.isError && <ErrorState error={offers.error} onRetry={() => offers.refetch()} />}
      {offers.data && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{t('admin.fields.title')}</th>
                <th>{t('admin.fields.type')}</th>
                <th>{t('admin.status')}</th>
                <th>{t('admin.fields.price')}</th>
                <th>{t('admin.fields.margin')}</th>
                <th>{t('admin.fields.available')}</th>
                <th>{t('admin.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {offers.data.map((offer) => (
                <tr key={offer.id}>
                  <td>
                    <Link to={`/admin/offers/${offer.id}`} className="section-link">
                      {localized(offer.title)}
                    </Link>
                    <div className="caption tertiary">{offer.slug}</div>
                  </td>
                  <td>
                    {offer.offerType === 'BUNDLE' ? t('offer.bundle') : t('offer.single')}
                    {offer.visibility === 'PLUS_ONLY' && <span className="badge badge-plus" style={{ marginLeft: 6 }}>{t('common.plus')}</span>}
                  </td>
                  <td>
                    <StatusPill status={offer.status} />
                  </td>
                  <td>{formatMoney(offer.sellingPrice)}</td>
                  <td style={{ color: offer.breakdown.sellable ? undefined : 'var(--color-danger)' }}>
                    {formatMoney(offer.breakdown.contributionMargin)} ({offer.breakdown.marginPercent}%)
                  </td>
                  <td>{offer.available}</td>
                  <td>
                    <div className="row" style={{ gap: 6 }}>
                      {offer.status !== 'PUBLISHED' && offer.status !== 'ARCHIVED' && (
                        <button type="button" className="btn btn-secondary btn-sm" onClick={() => change(offer, 'publish')}>
                          {t('admin.publish')}
                        </button>
                      )}
                      {offer.status === 'PUBLISHED' && (
                        <button type="button" className="btn btn-secondary btn-sm" onClick={() => change(offer, 'pause')}>
                          {t('admin.pause')}
                        </button>
                      )}
                      {offer.status !== 'ARCHIVED' && (
                        <button type="button" className="btn btn-ghost btn-sm" onClick={() => change(offer, 'archive')}>
                          {t('admin.archive')}
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
