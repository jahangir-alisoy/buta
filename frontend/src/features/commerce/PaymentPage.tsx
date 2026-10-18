import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { CircleAlert, CircleCheck, Clock } from 'lucide-react';
import { commerceApi } from '../../shared/api/endpoints';
import type { Order } from '../../shared/api/types';
import { formatMoney, formatTime } from '../../shared/format';
import { ErrorState, errorMessage } from '../../design-system/components/States';
import { PageLoader } from '../../app/PageLoader';

function useSecondsLeft(until: string | undefined): number {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);
  return until ? Math.max(0, Math.floor((new Date(until).getTime() - now) / 1000)) : 0;
}

function Outcome({ order }: { order: Order }) {
  const { t } = useTranslation();
  if (order.status === 'FULFILLED' || order.status === 'COMPLETED') {
    return (
      <div className="state">
        <div className="state-icon" style={{ color: 'var(--color-success)' }}>
          <CircleCheck size={28} />
        </div>
        <h1 className="section-title">{t('checkout.successTitle')}</h1>
        <p className="muted small">{t('checkout.successText')}</p>
        {order.pointsEarned > 0 && <p className="reason">{t('checkout.pointsToEarn', { points: order.pointsEarned })}</p>}
        <div className="row wrap" style={{ justifyContent: 'center' }}>
          <Link to={`/orders/${order.id}`} className="btn btn-primary">
            {t('checkout.viewOrder')}
          </Link>
          <Link to="/explore" className="btn btn-secondary">
            {t('checkout.continueShopping')}
          </Link>
        </div>
      </div>
    );
  }
  const failed = order.cancelReason === 'PAYMENT_FAILED';
  return (
    <div className="state">
      <div className="state-icon" style={{ color: 'var(--color-danger)' }}>
        <CircleAlert size={28} />
      </div>
      <h1 className="section-title">{failed ? t('checkout.failed') : t('checkout.expired')}</h1>
      <Link to="/cart" className="btn btn-primary">
        {t('cart.title')}
      </Link>
    </div>
  );
}

export default function PaymentPage() {
  const { orderId = '' } = useParams();
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const returnedFromStripe = params.get('stripe') === 'success';
  const stripeSessionId = params.get('session_id');
  const order = useQuery({
    queryKey: ['order', orderId],
    queryFn: () => commerceApi.order(orderId),
    staleTime: 0,
    refetchInterval: (query) => (returnedFromStripe && query.state.data?.status === 'PENDING_PAYMENT' ? 2000 : false),
  });
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!returnedFromStripe || !stripeSessionId) {
      return;
    }
    commerceApi
      .confirmStripeSession(stripeSessionId)
      .catch(() => undefined)
      .finally(() => {
        void queryClient.invalidateQueries({ queryKey: ['order', orderId] });
        void queryClient.invalidateQueries({ queryKey: ['orders'] });
        void queryClient.invalidateQueries({ queryKey: ['loyalty'] });
      });
  }, [returnedFromStripe, stripeSessionId, orderId, queryClient]);
  const [error, setError] = useState<string | null>(null);
  const secondsLeft = useSecondsLeft(order.data?.expiresAt);

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

  const confirm = async (outcome: 'SUCCEEDED' | 'FAILED') => {
    if (!data.payment) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const updated = await commerceApi.confirmSandbox(data.payment.id, outcome);
      queryClient.setQueryData(['order', orderId], updated);
      void queryClient.invalidateQueries({ queryKey: ['cart'] });
      void queryClient.invalidateQueries({ queryKey: ['orders'] });
      void queryClient.invalidateQueries({ queryKey: ['loyalty'] });
    } catch (failure) {
      setError(errorMessage(failure, t('errors.generic')));
      void order.refetch();
    } finally {
      setBusy(false);
    }
  };

  const payWithStripe = async () => {
    if (!data.payment) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const { url } = await commerceApi.stripeCheckout(data.payment.id);
      window.location.assign(url);
    } catch (failure) {
      setError(errorMessage(failure, t('errors.generic')));
      setBusy(false);
    }
  };

  const stripe = data.payment?.mode === 'STRIPE';

  return (
    <div className="container page" style={{ maxWidth: 560 }}>
      {data.status === 'PENDING_PAYMENT' && secondsLeft > 0 ? (
        <div className="card glass stack">
          <div className="row spread">
            <h1 className="section-title">{t('checkout.sandboxTitle')}</h1>
          </div>
          <p className="muted small">{t('checkout.sandboxText')}</p>
          <dl className="kv-list">
            <div className="kv">
              <dt>{t('orders.orderNumber', { number: data.orderNumber })}</dt>
              <dd>{formatMoney(data.total)}</dd>
            </div>
          </dl>
          <p className="row small muted" style={{ gap: 6 }}>
            <Clock size={16} aria-hidden="true" />
            {t('checkout.reservedUntil', { time: formatTime(data.expiresAt) })} ({Math.floor(secondsLeft / 60)}:{String(secondsLeft % 60).padStart(2, '0')})
          </p>
          {error && (
            <p className="alert alert-danger" role="alert">
              {error}
            </p>
          )}
          {stripe && returnedFromStripe ? (
            <p className="row small muted">{t('checkout.confirmingPayment')}</p>
          ) : stripe ? (
            <button type="button" className="btn btn-primary btn-lg btn-block" onClick={payWithStripe} disabled={busy}>
              {t('checkout.payWithCard', { amount: formatMoney(data.total) })}
            </button>
          ) : (
            <>
              <button type="button" className="btn btn-primary btn-lg btn-block" onClick={() => confirm('SUCCEEDED')} disabled={busy}>
                {t('checkout.simulateSuccess', { amount: formatMoney(data.total) })}
              </button>
              <button type="button" className="btn btn-secondary btn-block" onClick={() => confirm('FAILED')} disabled={busy}>
                {t('checkout.simulateFailure')}
              </button>
            </>
          )}
        </div>
      ) : data.status === 'PENDING_PAYMENT' ? (
        <div className="state">
          <h1 className="section-title">{t('checkout.expired')}</h1>
          <Link to="/cart" className="btn btn-primary">
            {t('cart.title')}
          </Link>
        </div>
      ) : (
        <Outcome order={data} />
      )}
    </div>
  );
}
