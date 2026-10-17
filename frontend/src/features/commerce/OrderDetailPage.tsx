import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ArrowLeft } from 'lucide-react';
import { commerceApi } from '../../shared/api/endpoints';
import type { Order } from '../../shared/api/types';
import { formatDate, formatDateTime, formatMoney } from '../../shared/format';
import { ErrorState, errorMessage } from '../../design-system/components/States';
import { ConfirmSheet } from '../../design-system/components/ConfirmSheet';
import { useToast } from '../../design-system/components/Toast';
import { PageLoader } from '../../app/PageLoader';
import { OrderStatusBadge } from './StatusBadge';
import { VoucherCard } from './VoucherCard';

export default function OrderDetailPage() {
  const { orderId = '' } = useParams();
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const order = useQuery({ queryKey: ['order', orderId], queryFn: () => commerceApi.order(orderId), staleTime: 0 });
  const [confirming, setConfirming] = useState<'refund' | 'cancel' | null>(null);
  const [busy, setBusy] = useState(false);

  if (order.isLoading) {
    return <PageLoader />;
  }
  if (order.isError || !order.data) {
    return (
      <div className="container page">
        <ErrorState error={order.error} onRetry={() => order.refetch()} />
      </div>
    );
  }
  const data = order.data;
  const refundable = data.items.some((item) => item.refundable);

  const run = async (action: () => Promise<Order>) => {
    setBusy(true);
    try {
      const updated = await action();
      queryClient.setQueryData(['order', orderId], updated);
      void queryClient.invalidateQueries({ queryKey: ['orders'] });
      void queryClient.invalidateQueries({ queryKey: ['loyalty'] });
      toast.show(t(`orders.statuses.${updated.status}`, { defaultValue: updated.status }));
    } catch (failure) {
      toast.show(errorMessage(failure, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
      setConfirming(null);
    }
  };

  return (
    <div className="container page" style={{ maxWidth: 900 }}>
      <Link to="/orders" className="btn btn-ghost btn-sm" style={{ marginBottom: 12 }}>
        <ArrowLeft size={16} aria-hidden="true" />
        {t('orders.title')}
      </Link>
      <header className="page-header">
        <div className="row spread wrap">
          <h1 className="page-title">{t('orders.orderNumber', { number: data.orderNumber })}</h1>
          <OrderStatusBadge status={data.status} />
        </div>
        <p className="muted small">{t('orders.placedOn', { date: formatDateTime(data.createdAt) })}</p>
        <div className="row wrap">
          {data.awaitingPayment && (
            <Link to={`/checkout/pay/${data.id}`} className="btn btn-primary">
              {t('orders.pay')}
            </Link>
          )}
          {data.cancellable && (
            <button type="button" className="btn btn-secondary" onClick={() => setConfirming('cancel')}>
              {t('orders.cancel')}
            </button>
          )}
          {refundable && (
            <button type="button" className="btn btn-secondary" onClick={() => setConfirming('refund')}>
              {t('orders.refund')}
            </button>
          )}
        </div>
      </header>
      <div className="two-col">
        <div className="stack">
          {data.items.map((item) => (
            <section key={item.id} className="card stack">
              <div className="row spread wrap">
                <h2 className="offer-title">
                  {item.quantity} × {item.title}
                </h2>
                <strong>{formatMoney(item.cashPaid)}</strong>
              </div>
              <div className="row wrap caption muted">
                <span>
                  {formatMoney(item.unitPrice)} × {item.quantity}
                </span>
                {item.pointsEarned > 0 && <span className="badge badge-plus">{t('common.points', { count: item.pointsEarned })}</span>}
                {item.refunded && <span className="badge badge-soft">{t('orders.statuses.REFUNDED')}</span>}
                {!item.refundable && item.refundBlockReason && !item.refunded && (
                  <span>{t(`orders.refundBlock.${item.refundBlockReason}`, { defaultValue: item.refundBlockReason })}</span>
                )}
              </div>
              {item.vouchers.length > 0 && (
                <div className="stack" style={{ gap: 10 }}>
                  <h3 className="small" style={{ fontWeight: 700 }}>
                    {t('orders.vouchers')}
                  </h3>
                  {item.vouchers.map((voucher) => (
                    <VoucherCard key={voucher.code} voucher={voucher} />
                  ))}
                </div>
              )}
            </section>
          ))}
        </div>
        <aside className="card stack detail-aside">
          <dl className="stack" style={{ gap: 8, margin: 0 }}>
            <div className="kv">
              <dt>{t('common.subtotal')}</dt>
              <dd>{formatMoney(data.subtotal)}</dd>
            </div>
            {data.pointsRedeemed > 0 && (
              <div className="kv">
                <dt>
                  {t('orders.pointsRedeemed')} ({data.pointsRedeemed})
                </dt>
                <dd>−{formatMoney(data.pointsDiscount)}</dd>
              </div>
            )}
            <div className="kv" style={{ fontSize: 18 }}>
              <dt>{t('common.total')}</dt>
              <dd>{formatMoney(data.total)}</dd>
            </div>
            {data.pointsEarned > 0 && (
              <div className="kv">
                <dt>{t('orders.pointsEarned')}</dt>
                <dd>{data.pointsEarned}</dd>
              </div>
            )}
            {data.payment && (
              <div className="kv">
                <dt>{t('orders.payment')}</dt>
                <dd>
                  {data.payment.mode} · {data.payment.status}
                </dd>
              </div>
            )}
          </dl>
          {data.refunds.length > 0 && (
            <div className="stack" style={{ gap: 6 }}>
              <h3 className="small" style={{ fontWeight: 700 }}>
                {t('orders.refunds')}
              </h3>
              {data.refunds.map((refund, index) => (
                <div key={index} className="kv">
                  <dt>{formatDate(refund.createdAt)}</dt>
                  <dd>{formatMoney(refund.amount)}</dd>
                </div>
              ))}
            </div>
          )}
        </aside>
      </div>
      <ConfirmSheet
        open={confirming !== null}
        title={confirming === 'refund' ? t('orders.refund') : t('orders.cancel')}
        confirmLabel={t('common.confirm')}
        danger
        busy={busy}
        onClose={() => setConfirming(null)}
        onConfirm={() => run(() => (confirming === 'refund' ? commerceApi.refund(data.id) : commerceApi.cancel(data.id)))}
      >
        {confirming === 'refund' ? t('orders.refundConfirm') : t('orders.cancel')}
      </ConfirmSheet>
    </div>
  );
}
