import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Award, Calendar, Clock, MapPin, ShoppingBag, Ticket } from 'lucide-react';
import { catalogApi } from '../../shared/api/endpoints';
import { ApiError } from '../../shared/api/client';
import type { OfferComponent, OfferDetail } from '../../shared/api/types';
import { formatDate, formatDateTime, formatMoney, formatPercent } from '../../shared/format';
import { ErrorState, EmptyState } from '../../design-system/components/States';
import { Stepper } from '../../design-system/components/Controls';
import { CategoryIcon } from '../../design-system/components/CategoryIcon';
import { PageLoader } from '../../app/PageLoader';
import { FavoriteButton } from './FavoriteButton';
import { RecommendedOffers } from './OfferCollection';
import { useAddToCart } from './useAddToCart';
import { track } from './track';

function ComponentItem({ component }: { component: OfferComponent }) {
  const { t } = useTranslation();
  return (
    <li className="component-item stack" style={{ gap: 10 }}>
      <div className="row spread wrap">
        <div className="stack" style={{ gap: 4 }}>
          <span className="offer-meta">
            <CategoryIcon iconKey={component.category.iconKey} size={14} />
            {t(`productTypes.${component.productType}`)}
            <span aria-hidden="true">·</span>
            <Link to={`/partners/${component.partner.slug}`} className="section-link">
              {component.partner.name}
            </Link>
          </span>
          <h3 className="offer-title">
            {component.quantity > 1 ? `${component.quantity} × ` : ''}
            {component.title}
          </h3>
        </div>
        <span className="muted small">{formatMoney(component.retailPrice)}</span>
      </div>
      <p className="muted small">{component.description}</p>
      <dl className="stack" style={{ gap: 6 }}>
        {component.eventStartsAt && (
          <div className="row small">
            <Calendar size={16} aria-hidden="true" />
            <span>{formatDateTime(component.eventStartsAt)}</span>
          </div>
        )}
        {component.venueName && (
          <div className="row small">
            <MapPin size={16} aria-hidden="true" />
            <span>
              {component.venueName}
              {component.venueAddress ? `, ${component.venueAddress}` : ''} · {component.city}
            </span>
          </div>
        )}
        <div className="row small">
          <Clock size={16} aria-hidden="true" />
          <span>
            {t('offer.validity')}: {formatDate(component.validFrom)} – {formatDate(component.validUntil)}
          </span>
        </div>
      </dl>
      <details>
        <summary className="small" style={{ cursor: 'pointer', fontWeight: 600 }}>
          {t('offer.redemption')} · {t('offer.terms')}
        </summary>
        <div className="stack small muted" style={{ gap: 8, marginTop: 8 }}>
          <p>
            <strong>{t(`redemptionMethods.${component.redemptionMethod}`)}.</strong> {component.redemptionInstructions}
          </p>
          <p>{component.terms}</p>
          {component.eligibilityNote && (
            <p>
              <strong>{t('offer.eligibility')}:</strong> {component.eligibilityNote}
            </p>
          )}
          <p>{component.refundable ? t('offer.refundable', { hours: component.refundDeadlineHours }) : t('offer.notRefundable')}</p>
        </div>
      </details>
    </li>
  );
}

interface PricePanelProps {
  detail: OfferDetail;
  quantity: number;
  setQuantity: (value: number) => void;
  onAdd: () => void;
  pending: boolean;
  onPurchaseVisible: (visible: boolean) => void;
}

function PricePanel({ detail, quantity, setQuantity, onAdd, pending, onPurchaseVisible }: PricePanelProps) {
  const purchaseRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const element = purchaseRef.current;
    if (!element) {
      return;
    }
    const observer = new IntersectionObserver(([entry]) => onPurchaseVisible(entry.isIntersecting || entry.boundingClientRect.top > 0));
    observer.observe(element);
    return () => observer.disconnect();
  }, [onPurchaseVisible]);

  const { t } = useTranslation();
  const offer = detail.offer;
  const maxQuantity = Math.max(1, Math.min(detail.maxPerOrder, offer.remaining ?? detail.maxPerOrder));
  const hasSaving = Number(offer.savings.amount) > 0;
  const availability =
    offer.availability === 'SOLD_OUT'
      ? t('offer.soldOut')
      : offer.availability === 'LOW_STOCK'
        ? t('offer.lowStock', { count: offer.remaining ?? 0 })
        : t('offer.available');
  return (
    <div className="card price-panel">
      {offer.offerType === 'BUNDLE' ? (
        <div className="comparison">
          <div className="comparison-cell">
            <span className="comparison-label">{t('offer.separately')}</span>
            <span className="comparison-value strike">{formatMoney(offer.referenceRetail)}</span>
          </div>
          <div className="comparison-cell is-highlight">
            <span className="comparison-label">{t('offer.bundlePrice')}</span>
            <span className="comparison-value">{formatMoney(offer.sellingPrice)}</span>
          </div>
        </div>
      ) : (
        <div className="price-head">
          <span className="price-big">{formatMoney(offer.sellingPrice)}</span>
          {hasSaving && <span className="price-reference strike">{formatMoney(offer.referenceRetail)}</span>}
        </div>
      )}
      {hasSaving && (
        <div className="savings-pill">
          {t('offer.youSave', { amount: formatMoney(offer.savings) })}
          <span className="savings-percent">{t('offer.savePercent', { percent: formatPercent(offer.savingsPercent) })}</span>
        </div>
      )}
      <dl className="kv-list">
        <div className="kv">
          <dt>{t('offer.availability')}</dt>
          <dd className={offer.availability === 'AVAILABLE' ? 'text-success' : 'text-warning'}>{availability}</dd>
        </div>
        <div className="kv">
          <dt>{t('offer.salesEnd')}</dt>
          <dd>{formatDate(offer.endsAt)}</dd>
        </div>
        <div className="kv">
          <dt>{t('offer.perOrder')}</dt>
          <dd>{t('offer.upTo', { count: maxQuantity })}</dd>
        </div>
      </dl>
      {offer.plusOnly && <span className="badge badge-plus price-panel-badge">{t('offer.plusOnly')}</span>}
      {offer.pointsPreview ? (
        <div className="points-note">
          <Award size={18} aria-hidden="true" />
          <div>
            <strong>{t('offer.pointsPreview', { points: offer.pointsPreview })}</strong>
            <p className="caption muted">{t('offer.pointsTerms')}</p>
          </div>
        </div>
      ) : null}
      {offer.purchasable ? (
        <div className="purchase-row" ref={purchaseRef}>
          <div className="quantity-row">
            <span className="small muted">{t('common.quantity')}</span>
            <Stepper value={quantity} max={maxQuantity} onChange={setQuantity} />
          </div>
          <button type="button" className="btn btn-primary btn-lg btn-block" onClick={onAdd} disabled={pending}>
            <ShoppingBag size={18} aria-hidden="true" />
            {pending ? t('offer.adding') : t('offer.addToCart')}
          </button>
        </div>
      ) : (
        <div className="stack" style={{ gap: 8 }}>
          <button type="button" className="btn btn-secondary btn-lg btn-block" disabled>
            {t(`offer.blocked.${offer.purchaseBlockReason ?? 'NOT_SELLABLE'}`, { defaultValue: t('offer.blocked.NOT_SELLABLE') })}
          </button>
          {offer.purchaseBlockReason === 'PLUS_REQUIRED' && (
            <p className="small muted text-center">
              {t('offer.plusOnlyText')}{' '}
              <Link to="/rewards" className="section-link">
                {t('offer.getPlus')}
              </Link>
            </p>
          )}
        </div>
      )}
    </div>
  );
}

export default function OfferDetailPage() {
  const { slug = '' } = useParams();
  const [params] = useSearchParams();
  const recommendationRequestId = params.get('rec') ?? undefined;
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [quantity, setQuantity] = useState(1);
  const [purchaseVisible, setPurchaseVisible] = useState(true);
  const { add, pending } = useAddToCart();
  const detail = useQuery({ queryKey: ['offer', slug], queryFn: () => catalogApi.offer(slug) });
  const offerId = detail.data?.offer.id;
  const similar = useQuery({
    queryKey: ['recommendations', 'OFFER_DETAIL', offerId],
    queryFn: () => catalogApi.recommendations('OFFER_DETAIL', 8, offerId),
    enabled: Boolean(offerId),
  });

  useEffect(() => {
    if (detail.data) {
      track({ type: 'DETAIL_VIEW', offerId: detail.data.offer.id, categoryId: detail.data.offer.category.id });
    }
  }, [detail.data]);

  useEffect(() => setQuantity(1), [slug]);

  if (detail.isLoading) {
    return <PageLoader />;
  }
  if (detail.error instanceof ApiError && detail.error.status === 404) {
    return (
      <div className="container page">
        <EmptyState
          title={t('offer.notFound')}
          icon={<Ticket size={24} />}
          action={
            <Link to="/explore" className="btn btn-primary">
              {t('cart.browse')}
            </Link>
          }
        />
      </div>
    );
  }
  if (detail.isError || !detail.data) {
    return (
      <div className="container page">
        <ErrorState error={detail.error} onRetry={() => detail.refetch()} />
      </div>
    );
  }

  const data = detail.data;
  const offer = data.offer;
  const onAdd = () => add({ offerId: offer.id, quantity, recommendationRequestId });

  return (
    <div className={`container page${offer.purchasable ? ' has-sticky' : ''}`}>
      <button type="button" className="btn btn-ghost btn-sm" onClick={() => (window.history.length > 1 ? navigate(-1) : navigate('/explore'))} style={{ marginBottom: 12 }}>
        <ArrowLeft size={16} aria-hidden="true" />
        {t('common.back')}
      </button>
      <div className="detail-layout">
        <div className="detail-top stack" style={{ gap: 20 }}>
          <div className="detail-media" style={{ position: 'relative' }}>
            {offer.imagePath && <img src={offer.imagePath} alt="" />}
            <div className="offer-fav">
              <FavoriteButton offerId={offer.id} favorite={offer.favorite} />
            </div>
          </div>
          <header className="stack" style={{ gap: 8 }}>
            <div className="row wrap" style={{ gap: 6 }}>
              <span className="badge badge-soft">{offer.offerType === 'BUNDLE' ? t('offer.bundle') : t('offer.single')}</span>
              <Link to={`/categories/${offer.category.slug}`} className="badge badge-soft">
                {offer.category.name}
              </Link>
              {offer.campaign && <span className="badge badge-accent">{offer.campaign.name}</span>}
            </div>
            <h1 className="page-title">{offer.title}</h1>
            <p className="muted">{offer.subtitle}</p>
          </header>
        </div>
        <aside className="detail-aside">
          <PricePanel detail={data} quantity={quantity} setQuantity={setQuantity} onAdd={onAdd} pending={pending} onPurchaseVisible={setPurchaseVisible} />
        </aside>
        <div className="detail-rest stack" style={{ gap: 24 }}>
          <section className="stack" style={{ gap: 8 }}>
            <h2 className="section-title">{t('offer.description')}</h2>
            <p className="muted" style={{ whiteSpace: 'pre-line' }}>
              {data.description}
            </p>
          </section>
          <section className="stack" style={{ gap: 12 }}>
            <h2 className="section-title">{t('offer.whatsIncluded')}</h2>
            <ul className="component-list" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
              {data.components.map((component) => (
                <ComponentItem key={component.productId} component={component} />
              ))}
            </ul>
          </section>
        </div>
      </div>
      {similar.data && similar.data.items.length > 0 && (
        <section className="section">
          <div className="section-header">
            <h2 className="section-title">{t('offer.similar')}</h2>
          </div>
          <RecommendedOffers data={similar.data} layout="rail" />
        </section>
      )}
      {offer.purchasable && !purchaseVisible && (
        <div className="sticky-action glass">
          <div className="price" style={{ flex: 1 }}>
            <span className="price-current">{formatMoney(offer.sellingPrice)}</span>
            {Number(offer.savings.amount) > 0 && <span className="price-reference strike">{formatMoney(offer.referenceRetail)}</span>}
          </div>
          <button type="button" className="btn btn-primary" onClick={onAdd} disabled={pending}>
            {pending ? t('offer.adding') : t('offer.addToCart')}
          </button>
        </div>
      )}
    </div>
  );
}
