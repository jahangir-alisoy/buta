import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { formatMoney, formatNumber } from '../../../shared/format';
import { CardGridSkeleton, EmptyState, ErrorState } from '../../../design-system/components/States';
import { adminApi } from '../api';
import { AdminHeader } from '../forms';

export default function AdminMetricsPage() {
  const { t } = useTranslation();
  const metrics = useQuery({ queryKey: ['admin', 'metrics'], queryFn: adminApi.metrics, staleTime: 0 });
  return (
    <>
      <AdminHeader title={t('admin.nav.metrics')} />
      {metrics.isLoading && <CardGridSkeleton count={2} />}
      {metrics.isError && <ErrorState error={metrics.error} onRetry={() => metrics.refetch()} />}
      {metrics.data && metrics.data.length === 0 && <EmptyState title={t('admin.metrics.empty')} />}
      {metrics.data && metrics.data.length > 0 && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{t('admin.metrics.variant')}</th>
                <th>{t('admin.metrics.impressions')}</th>
                <th>{t('admin.metrics.clicks')}</th>
                <th>{t('admin.metrics.ctr')}</th>
                <th>{t('admin.metrics.cartAdds')}</th>
                <th>{t('admin.metrics.purchases')}</th>
                <th>{t('admin.metrics.conversion')}</th>
                <th>{t('admin.metrics.revenue')}</th>
                <th>{t('admin.metrics.margin')}</th>
              </tr>
            </thead>
            <tbody>
              {metrics.data.map((row) => (
                <tr key={row.variant}>
                  <td>{row.variant}</td>
                  <td>{formatNumber(row.impressions)}</td>
                  <td>{formatNumber(row.clicks)}</td>
                  <td>{row.clickThroughRatePercent}%</td>
                  <td>{formatNumber(row.cartAdds)}</td>
                  <td>{formatNumber(row.purchases)}</td>
                  <td>{row.conversionRatePercent}%</td>
                  <td>{formatMoney(row.revenue)}</td>
                  <td>{formatMoney(row.contributionMargin)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
