import { useEffect, useState } from 'react';
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Search } from 'lucide-react';
import { formatDateTime, formatMoney } from '../../../shared/format';
import type { Order } from '../../../shared/api/types';
import { CardGridSkeleton, ErrorState, Skeleton, errorMessage } from '../../../design-system/components/States';
import { Sheet } from '../../../design-system/components/Sheet';
import { useToast } from '../../../design-system/components/Toast';
import { OrderStatusBadge } from '../../commerce/StatusBadge';
import { VoucherCard } from '../../commerce/VoucherCard';
import { adminApi } from '../api';
import { AdminHeader, CheckField, FormField } from '../forms';

const STATUSES = ['PENDING_PAYMENT', 'FULFILLED', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'PARTIALLY_REFUNDED', 'REFUNDED'];

function OrderPanel({ orderId }: { orderId: string }) {
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const order = useQuery({ queryKey: ['admin', 'order', orderId], queryFn: () => adminApi.order(orderId), staleTime: 0 });
  const [selected, setSelected] = useState<string[]>([]);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);

  if (order.isLoading) {
    return <Skeleton height={200} radius={16} />;
  }
  if (!order.data) {
    return <ErrorState error={order.error} onRetry={() => order.refetch()} />;
  }
  const data: Order = order.data;
  const eligible = data.items.filter((item) => !item.refunded);

  const refund = async (itemIds: string[] | undefined) => {
    setBusy(true);
    try {
      const updated = await adminApi.refund(data.id, itemIds, reason.trim());
      queryClient.setQueryData(['admin', 'order', orderId], updated);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'orders'] });
      setSelected([]);
      toast.show(t(`orders.statuses.${updated.status}`, { defaultValue: updated.status }));
    } catch (error) {
      toast.show(errorMessage(error, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="stack">
      <div className="row spread wrap">
        <span className="small muted">{formatDateTime(data.createdAt)}</span>
        <OrderStatusBadge status={data.status} />
      </div>
      <dl className="stack" style={{ gap: 6, margin: 0 }}>
        <div className="kv">
          <dt>{t('common.total')}</dt>
          <dd>{formatMoney(data.total)}</dd>
        </div>
        <div className="kv">
          <dt>{t('orders.pointsRedeemed')}</dt>
          <dd>{data.pointsRedeemed}</dd>
        </div>
        <div className="kv">
          <dt>{t('orders.pointsEarned')}</dt>
          <dd>{data.pointsEarned}</dd>
        </div>
        {data.payment && (
          <div className="kv">
            <dt>{t('orders.payment')}</dt>
            <dd>
              {data.payment.provider} · {data.payment.status} · {formatMoney(data.payment.refunded)}
            </dd>
          </div>
        )}
      </dl>
      {data.items.map((item) => (
        <div key={item.id} className="card stack" style={{ gap: 8 }}>
          <div className="row spread wrap">
            {eligible.includes(item) ? (
              <CheckField
                label={`${item.quantity} × ${item.title}`}
                checked={selected.includes(item.id)}
                onChange={(checked) => setSelected(checked ? [...selected, item.id] : selected.filter((id) => id !== item.id))}
              />
            ) : (
              <span>
                {item.quantity} × {item.title}
              </span>
            )}
            <strong>{formatMoney(item.cashPaid)}</strong>
          </div>
          {item.vouchers.map((voucher) => (
            <VoucherCard key={voucher.code} voucher={voucher} />
          ))}
        </div>
      ))}
      {eligible.length > 0 && ['FULFILLED', 'COMPLETED', 'PARTIALLY_REFUNDED'].includes(data.status) && (
        <div className="stack">
          <FormField id="refund-reason" label={t('admin.fields.reason')}>
            <input id="refund-reason" className="input" maxLength={200} value={reason} onChange={(event) => setReason(event.target.value)} />
          </FormField>
          <div className="row wrap">
            <button type="button" className="btn btn-danger btn-sm" disabled={busy || selected.length === 0} onClick={() => refund(selected)}>
              {t('admin.orders.refundSelected')}
            </button>
            <button type="button" className="btn btn-secondary btn-sm" disabled={busy} onClick={() => refund(undefined)}>
              {t('admin.orders.refundAll')}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function AdminOrdersPage() {
  const { t } = useTranslation();
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<{ id: string; number: string } | null>(null);
  const orders = useQuery({
    queryKey: ['admin', 'orders', status, query, page],
    queryFn: () => adminApi.orders(status, query, page),
    placeholderData: keepPreviousData,
    staleTime: 0,
  });

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setQuery(search.trim());
      setPage(0);
    }, 350);
    return () => window.clearTimeout(timer);
  }, [search]);

  return (
    <>
      <AdminHeader title={t('admin.nav.orders')} />
      <div className="toolbar">
        <label className="search-bar">
          <Search size={18} />
          <span className="visually-hidden">{t('admin.search')}</span>
          <input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="BT-…" />
        </label>
        <select
          className="select"
          aria-label={t('admin.status')}
          value={status}
          onChange={(event) => {
            setStatus(event.target.value);
            setPage(0);
          }}
        >
          <option value="">{t('admin.orders.allStatuses')}</option>
          {STATUSES.map((item) => (
            <option key={item} value={item}>
              {t(`orders.statuses.${item}`)}
            </option>
          ))}
        </select>
      </div>
      {orders.isLoading && <CardGridSkeleton count={2} />}
      {orders.isError && <ErrorState error={orders.error} onRetry={() => orders.refetch()} />}
      {orders.data && (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>{t('admin.fields.createdAt')}</th>
                  <th>{t('admin.fields.title')}</th>
                  <th>{t('common.total')}</th>
                  <th>{t('admin.status')}</th>
                  <th>{t('admin.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {orders.data.items.map((order) => (
                  <tr key={order.id}>
                    <td>{order.orderNumber}</td>
                    <td>{formatDateTime(order.createdAt)}</td>
                    <td>
                      {order.firstItemTitle}
                      <div className="caption tertiary">{t('orders.items', { count: order.itemCount })}</div>
                    </td>
                    <td>{formatMoney(order.total)}</td>
                    <td>
                      <OrderStatusBadge status={order.status} />
                    </td>
                    <td>
                      <button type="button" className="btn btn-secondary btn-sm" onClick={() => setSelected({ id: order.id, number: order.orderNumber })}>
                        {t('admin.details')}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="row spread" style={{ marginTop: 12 }}>
            <button type="button" className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
              ←
            </button>
            <span className="caption muted">
              {page + 1} {t('common.of')} {Math.max(1, orders.data.totalPages)}
            </span>
            <button type="button" className="btn btn-ghost btn-sm" disabled={page + 1 >= orders.data.totalPages} onClick={() => setPage(page + 1)}>
              →
            </button>
          </div>
        </>
      )}
      <Sheet open={selected !== null} title={selected ? t('orders.orderNumber', { number: selected.number }) : ''} onClose={() => setSelected(null)}>
        {selected && <OrderPanel key={selected.id} orderId={selected.id} />}
      </Sheet>
    </>
  );
}
