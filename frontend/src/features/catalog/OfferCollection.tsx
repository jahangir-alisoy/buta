import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ChevronRight } from 'lucide-react';
import type { OfferCard as Offer, RecommendationItem, Recommendations } from '../../shared/api/types';
import { OfferCard } from './OfferCard';
import { reasonText } from './reasons';
import { track } from './track';

interface RailProps {
  title: string;
  offers: Offer[];
  link?: string;
}

export function OfferRail({ title, offers, link }: RailProps) {
  const { t } = useTranslation();
  if (offers.length === 0) {
    return null;
  }
  return (
    <section className="section">
      <div className="section-header">
        <h2 className="section-title">{title}</h2>
        {link && (
          <Link to={link} className="section-link row" style={{ gap: 2 }}>
            {t('common.seeAll')}
            <ChevronRight size={16} aria-hidden="true" />
          </Link>
        )}
      </div>
      <div className="offer-rail">
        {offers.map((offer) => (
          <OfferCard key={offer.id} offer={offer} />
        ))}
      </div>
    </section>
  );
}

export function OfferGrid({ offers }: { offers: Offer[] }) {
  return (
    <div className="offer-grid">
      {offers.map((offer) => (
        <OfferCard key={offer.id} offer={offer} />
      ))}
    </div>
  );
}

interface RecommendedProps {
  data: Recommendations;
  layout: 'rail' | 'grid';
}

export function RecommendedOffers({ data, layout }: RecommendedProps) {
  const { t } = useTranslation();
  const open = (item: RecommendationItem) => () =>
    track({ type: 'RECOMMENDATION_CLICK', offerId: item.offer.id, categoryId: item.offer.category.id, requestId: data.requestId, placement: data.placement });
  return (
    <div className={layout === 'rail' ? 'offer-rail' : 'offer-grid'}>
      {data.items.map((item) => (
        <OfferCard
          key={item.offer.id}
          offer={item.offer}
          reason={reasonText(t, item.reasonCode, item.reasonParams)}
          recommendationRequestId={data.requestId}
          onOpen={open(item)}
        />
      ))}
    </div>
  );
}
