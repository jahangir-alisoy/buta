import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Calendar, Sparkle } from 'lucide-react';
import type { OfferCard as Offer } from '../../shared/api/types';
import { formatDateTime, formatMoney, formatPercent } from '../../shared/format';
import { CategoryIcon } from '../../design-system/components/CategoryIcon';
import { FavoriteButton } from './FavoriteButton';

interface OfferCardProps {
  offer: Offer;
  reason?: string;
  recommendationRequestId?: string;
  onOpen?: () => void;
}

export function AvailabilityBadge({ offer }: { offer: Offer }) {
  const { t } = useTranslation();
  if (offer.availability === 'SOLD_OUT') {
    return <span className="badge badge-danger">{t('offer.soldOut')}</span>;
  }
  if (offer.availability === 'LOW_STOCK') {
    return <span className="badge badge-warning">{t('offer.lowStock', { count: offer.remaining ?? 0 })}</span>;
  }
  return null;
}

export function OfferCard({ offer, reason, recommendationRequestId, onOpen }: OfferCardProps) {
  const { t } = useTranslation();
  const hasSaving = Number(offer.savings.amount) > 0;
  return (
    <article className="offer-card">
      <div className="offer-media">
        {offer.imagePath && <img src={offer.imagePath} alt="" loading="lazy" decoding="async" />}
        <div className="offer-media-badges">
          {offer.offerType === 'BUNDLE' && <span className="badge badge-soft">{t('offer.bundle')}</span>}
          {hasSaving && <span className="badge badge-accent">{t('offer.savePercent', { percent: formatPercent(offer.savingsPercent) })}</span>}
          {offer.plusOnly && <span className="badge badge-plus">{t('offer.plusOnly')}</span>}
          <AvailabilityBadge offer={offer} />
        </div>
      </div>
      <div className="offer-fav">
        <FavoriteButton offerId={offer.id} favorite={offer.favorite} />
      </div>
      <div className="offer-body">
        <div className="offer-meta">
          <CategoryIcon iconKey={offer.category.iconKey} size={14} />
          <span>{offer.category.name}</span>
        </div>
        <h3 className="offer-title">
          <Link to={recommendationRequestId ? `/offers/${offer.slug}?rec=${recommendationRequestId}` : `/offers/${offer.slug}`} onClick={onOpen}>
            {offer.title}
          </Link>
        </h3>
        <p className="offer-partners">{offer.partners.map((partner) => partner.name).join(' + ')}</p>
        <p className="offer-subtitle">{offer.subtitle}</p>
        {offer.eventStartsAt && (
          <span className="offer-meta">
            <Calendar size={14} aria-hidden="true" />
            {formatDateTime(offer.eventStartsAt)}
          </span>
        )}
        {reason && (
          <span className="reason">
            <Sparkle size={14} aria-hidden="true" />
            {reason}
          </span>
        )}
        <div className="offer-footer">
          <div className="price">
            <span className="price-current">{formatMoney(offer.sellingPrice)}</span>
            {hasSaving && <span className="price-reference strike">{formatMoney(offer.referenceRetail)}</span>}
          </div>
          {offer.pointsPreview ? <span className="badge badge-plus">{t('offer.pointsPreview', { points: offer.pointsPreview })}</span> : null}
        </div>
      </div>
    </article>
  );
}
