import { useEffect, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router';
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Award, Lock } from 'lucide-react';
import { commerceApi } from '../../shared/api/endpoints';
import type { Quote } from '../../shared/api/types';
import { formatMoney } from '../../shared/format';
import { ErrorState, errorMessage } from '../../design-system/components/States';
import { PageLoader } from '../../app/PageLoader';
import { useAuth } from '../auth/AuthProvider';

function normalizePoints(value: number, quote: Quote | undefined): number {
  if (!quote || !quote.points.allowed || !Number.isFinite(value) || value <= 0) {
    return 0;
  }
  const step = Math.max(1, quote.points.step);
  const capped = Math.min(value, quote.points.maxRedeemable);
  const stepped = Math.floor(capped / step) * step;
  return stepped >= quote.points.minimum ? stepped : 0;
}

function PointsControl({ quote, points, onChange }: { quote: Quote; points: number; onChange: (value: number) => void }) {
  const { t } = useTranslation();
  const { isPlus } = useAuth();
  const value = formatMoney(Number(quote.points.pointValue) * 100);
  if (!isPlus) {
    return <p className="small muted">{t('checkout.pointsPlusOnly')}</p>;
  }
  if (!quote.points.allowed || quote.points.maxRedeemable < quote.points.minimum) {
    return (
      <div className="stack" style={{ gap: 4 }}>
        <span className="small">{t('checkout.pointsAvailable', { points: quote.points.available })}</span>
        <p className="small muted">{t('checkout.pointsNotEnough')}</p>
      </div>
    );
  }
  return (
    <div className="stack" style={{ gap: 8 }}>
      <div className="row spread">
        <label htmlFor="points" className="small" style={{ fontWeight: 600 }}>
          {t('checkout.points')}
        </label>
        <span className="caption muted">{t('checkout.pointsAvailable', { points: quote.points.available })}</span>
      </div>
      <input
        id="points"
        type="range"
        min={0}
        max={quote.points.maxRedeemable}
        step={quote.points.step}
        value={points}
        onChange={(event) => onChange(Number(event.target.value))}
        style={{ width: '100%', accentColor: 'var(--color-accent)' }}
      />
      <div className="row spread">
        <input
          className="input"
          type="number"
          inputMode="numeric"
          min={0}
          max={quote.points.maxRedeemable}
          step={quote.points.step}
          value={points}
          onChange={(event) => onChange(Number(event.target.value))}
          aria-label={t('checkout.points')}
          style={{ maxWidth: 140 }}
        />
        <span className="caption muted">{t('checkout.pointsHint', { max: quote.points.maxRedeemable, value })}</span>
      </div>
    </div>
  );
}

export default function CheckoutPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [requested, setRequested] = useState(0);
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID());
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const quote = useQuery({
    queryKey: ['quote', requested],
    queryFn: () => commerceApi.quote(requested),
    placeholderData: keepPreviousData,
    staleTime: 0,
  });
  const points = normalizePoints(requested, quote.data);

  useEffect(() => setIdempotencyKey(crypto.randomUUID()), [points]);

  if (quote.isLoading) {
    return <PageLoader />;
  }
  if (quote.isError && !quote.data) {
    return (
      <div className="container page stack">
        <ErrorState error={quote.error} onRetry={() => quote.refetch()} />
        <Link to="/cart" className="btn btn-secondary" style={{ alignSelf: 'center' }}>
          {t('cart.title')}
        </Link>
      </div>
    );
  }
  if (!quote.data || quote.data.lines.length === 0) {
    return <Navigate to="/cart" replace />;
  }

  const data = quote.data;
  const place = async () => {
    setError(null);
    setPlacing(true);
    try {
      const order = await commerceApi.checkout(points, idempotencyKey);
      void queryClient.invalidateQueries({ queryKey: ['cart'] });
      navigate(`/checkout/pay/${order.id}`, { replace: true });
    } catch (failure) {
      setError(errorMessage(failure, t('errors.generic')));
      setIdempotencyKey(crypto.randomUUID());
      void quote.refetch();
      void queryClient.invalidateQueries({ queryKey: ['cart'] });
      setPlacing(false);
    }
  };

  return (
    <div className="container page">
      <header className="page-header">
        <h1 className="page-title">{t('checkout.title')}</h1>
        <p className="muted">{t('checkout.review')}</p>
      </header>
      <div className="two-col">
        <div className="card stack">
          <ul className="stack" style={{ listStyle: 'none', margin: 0, padding: 0, gap: 12 }}>
            {data.lines.map((line) => (
              <li key={line.offerId} className="row spread">
                <span>
                  {line.quantity} × {line.title}
                </span>
                <strong>{formatMoney(line.lineTotal)}</strong>
              </li>
            ))}
          </ul>
          <hr className="divider" />
          <PointsControl quote={data} points={requested} onChange={setRequested} />
        </div>
        <aside className="card stack detail-aside">
          <dl className="stack" style={{ gap: 8, margin: 0 }}>
            <div className="kv">
              <dt>{t('common.subtotal')}</dt>
              <dd>{formatMoney(data.subtotal)}</dd>
            </div>
            {data.pointsToRedeem > 0 && (
              <div className="kv">
                <dt>{t('checkout.discount')}</dt>
                <dd>−{formatMoney(data.pointsDiscount)}</dd>
              </div>
            )}
            <div className="kv" style={{ fontSize: 18 }}>
              <dt>{t('checkout.toPay')}</dt>
              <dd>{formatMoney(data.total)}</dd>
            </div>
          </dl>
          {data.pointsToEarn ? (
            <p className="reason">
              <Award size={16} aria-hidden="true" />
              {t('checkout.pointsToEarn', { points: data.pointsToEarn })}
            </p>
          ) : null}
          {error && (
            <p className="alert alert-danger" role="alert">
              {error}
            </p>
          )}
          <button type="button" className="btn btn-primary btn-lg btn-block" onClick={place} disabled={placing || quote.isFetching}>
            <Lock size={18} aria-hidden="true" />
            {placing ? t('checkout.placing') : t('checkout.placeOrder')}
          </button>
          {data.paymentMode === 'SANDBOX' && <span className="badge badge-soft" style={{ alignSelf: 'center' }}>{t('common.sandbox')}</span>}
        </aside>
      </div>
    </div>
  );
}
