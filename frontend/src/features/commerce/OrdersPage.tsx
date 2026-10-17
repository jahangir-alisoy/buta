import { Link } from 'react-router';
import { useInfiniteQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ChevronRight, Receipt } from 'lucide-react';
import { commerceApi } from '../../shared/api/endpoints';
import { formatDate, formatMoney } from '../../shared/format';
import { CardGridSkeleton, EmptyState, ErrorState } from '../../design-system/components/States';
import { OrderStatusBadge } from './StatusBadge';

export default function OrdersPage() {
  const { t } = useTranslation();
  const orders = useInfiniteQuery({
    queryKey: ['orders'],
    queryFn: ({ pageParam }) => commerceApi.orders(pageParam),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
    staleTime: 0,
  });
  const items = orders.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <div className="container page" style={{ maxWidth: 820 }}>
      <header className="page-header">
        <h1 className="page-title">{t('orders.title')}</h1>
      </header>
      {orders.isLoading && <CardGridSkeleton count={2} />}
      {orders.isError && <ErrorState error={orders.error} onRetry={() => orders.refetch()} />}
      {orders.data && items.length === 0 && (
        <EmptyState
          icon={<Receipt size={24} />}
          title={t('orders.empty')}
          action={
            <Link to="/explore" className="btn btn-primary">
              {t('cart.browse')}
            </Link>
          }
        />
      )}
      {items.length > 0 && (
        <div className="card menu-list" style={{ padding: 0 }}>
          {items.map((order) => (
            <Link key={order.id} to={`/orders/${order.id}`} style={{ minHeight: 72 }}>
              <div className="stack" style={{ gap: 2, minWidth: 0 }}>
                <strong>{order.firstItemTitle ?? t('orders.orderNumber', { number: order.orderNumber })}</strong>
                <span className="caption tertiary">
                  {t('orders.orderNumber', { number: order.orderNumber })} · {t('orders.placedOn', { date: formatDate(order.createdAt) })} ·{' '}
                  {t('orders.items', { count: order.itemCount })}
                </span>
              </div>
              <div className="stack" style={{ gap: 4, marginLeft: 'auto', alignItems: 'flex-end' }}>
                <strong>{formatMoney(order.total)}</strong>
                <OrderStatusBadge status={order.status} />
              </div>
              <ChevronRight size={18} className="chevron" aria-hidden="true" style={{ marginLeft: 8 }} />
            </Link>
          ))}
        </div>
      )}
      {orders.hasNextPage && (
        <div className="row" style={{ justifyContent: 'center', marginTop: 24 }}>
          <button type="button" className="btn btn-secondary" onClick={() => orders.fetchNextPage()} disabled={orders.isFetchingNextPage}>
            {t('common.loadMore')}
          </button>
        </div>
      )}
    </div>
  );
}
