import { Link, useNavigate } from 'react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ShoppingBag, Trash2, TriangleAlert } from 'lucide-react';
import { commerceApi } from '../../shared/api/endpoints';
import type { Cart, CartLine } from '../../shared/api/types';
import { formatMoney } from '../../shared/format';
import { CardGridSkeleton, EmptyState, ErrorState, errorMessage } from '../../design-system/components/States';
import { Stepper } from '../../design-system/components/Controls';
import { useToast } from '../../design-system/components/Toast';

function Line({ line, onQuantity, onRemove, busy }: { line: CartLine; onQuantity: (value: number) => void; onRemove: () => void; busy: boolean }) {
  const { t } = useTranslation();
  const offer = line.offer;
  return (
    <li className="list-row" style={{ alignItems: 'flex-start' }}>
      {offer?.imagePath ? <img className="thumb" src={offer.imagePath} alt="" /> : <span className="thumb" />}
      <div className="stack" style={{ gap: 6, flex: 1, minWidth: 0 }}>
        {offer ? (
          <Link to={`/offers/${offer.slug}`} className="offer-title">
            {offer.title}
          </Link>
        ) : (
          <span className="offer-title">—</span>
        )}
        <span className="small muted">
          {formatMoney(line.unitPrice)} × {line.quantity}
        </span>
        {line.priceChanged && <span className="caption badge badge-warning">{t('cart.priceChanged', { old: formatMoney(line.unitPriceAtAdd) })}</span>}
        {line.issue && (
          <span className="alert alert-warning small">
            <TriangleAlert size={16} aria-hidden="true" />
            {t(`cart.issues.${line.issue}`, { defaultValue: line.issue })}
          </span>
        )}
        <div className="row spread wrap">
          <Stepper value={line.quantity} max={Math.max(line.quantity, line.maxQuantity)} onChange={onQuantity} disabled={busy} />
          <button type="button" className="btn btn-ghost btn-sm" onClick={onRemove} disabled={busy}>
            <Trash2 size={16} aria-hidden="true" />
            {t('common.remove')}
          </button>
        </div>
      </div>
      <strong className="price-current" style={{ fontSize: 17 }}>
        {formatMoney(line.lineTotal)}
      </strong>
    </li>
  );
}

export default function CartPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const cart = useQuery({ queryKey: ['cart'], queryFn: commerceApi.cart, staleTime: 0 });
  const onSuccess = (data: Cart) => queryClient.setQueryData(['cart'], data);
  const onError = (error: unknown) => toast.show(errorMessage(error, t('errors.generic')), 'error');
  const update = useMutation({ mutationFn: ({ id, quantity }: { id: string; quantity: number }) => commerceApi.updateItem(id, quantity), onSuccess, onError });
  const remove = useMutation({ mutationFn: (id: string) => commerceApi.removeItem(id), onSuccess, onError });
  const busy = update.isPending || remove.isPending;

  return (
    <div className="container page">
      <header className="page-header">
        <h1 className="page-title">{t('cart.title')}</h1>
      </header>
      {cart.isLoading && <CardGridSkeleton count={2} />}
      {cart.isError && <ErrorState error={cart.error} onRetry={() => cart.refetch()} />}
      {cart.data && cart.data.items.length === 0 && (
        <EmptyState
          icon={<ShoppingBag size={24} />}
          title={t('cart.empty')}
          text={t('cart.emptyHint')}
          action={
            <Link to="/explore" className="btn btn-primary">
              {t('cart.browse')}
            </Link>
          }
        />
      )}
      {cart.data && cart.data.items.length > 0 && (
        <div className="two-col">
          <ul className="card" style={{ listStyle: 'none', margin: 0, padding: 0 }}>
            {cart.data.items.map((line) => (
              <Line
                key={line.id}
                line={line}
                busy={busy}
                onQuantity={(quantity) => update.mutate({ id: line.id, quantity })}
                onRemove={() => remove.mutate(line.id)}
              />
            ))}
          </ul>
          <aside className="card stack detail-aside">
            <dl className="kv">
              <dt>{t('cart.items', { count: cart.data.itemCount })}</dt>
              <dd>{formatMoney(cart.data.subtotal)}</dd>
            </dl>
            {cart.data.pointsPreview ? <p className="reason">{t('cart.pointsPreview', { points: cart.data.pointsPreview })}</p> : null}
            <button type="button" className="btn btn-primary btn-lg btn-block" disabled={!cart.data.checkoutReady || busy} onClick={() => navigate('/checkout')}>
              {t('cart.checkout')}
            </button>
          </aside>
        </div>
      )}
    </div>
  );
}
