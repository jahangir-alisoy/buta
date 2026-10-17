import { lazy, Suspense, useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Award, Search } from 'lucide-react';
import { catalogApi } from '../../shared/api/endpoints';
import { CardGridSkeleton, ErrorState } from '../../design-system/components/States';
import { CategoryIcon } from '../../design-system/components/CategoryIcon';
import { usePreferences } from '../../app/PreferencesProvider';
import { useAuth } from '../auth/AuthProvider';
import { OfferRail, RecommendedOffers } from './OfferCollection';
import { OfferCard } from './OfferCard';

const HeroScene = lazy(() => import('./HeroScene'));

function useDeferredHero(): boolean {
  const [show, setShow] = useState(false);
  useEffect(() => {
    const connection = (navigator as Navigator & { connection?: { saveData?: boolean } }).connection;
    if (connection?.saveData) {
      return;
    }
    const timer = window.setTimeout(() => setShow(true), 300);
    return () => window.clearTimeout(timer);
  }, []);
  return show;
}

function Hero() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { theme } = usePreferences();
  const [query, setQuery] = useState('');
  const showScene = useDeferredHero();

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const value = query.trim();
    navigate(value ? `/explore?q=${encodeURIComponent(value)}` : '/explore');
  };

  return (
    <section className="hero">
      <div className="container hero-inner">
        <div className="hero-copy">
          <span className="hero-eyebrow">{t('home.heroEyebrow')}</span>
          <h1 className="hero-title">{t('home.heroTitle')}</h1>
          <p className="hero-subtitle">{t('home.heroSubtitle')}</p>
          <form role="search" onSubmit={submit}>
            <label className="search-bar">
              <Search size={18} />
              <span className="visually-hidden">{t('common.search')}</span>
              <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t('home.searchPlaceholder')} />
            </label>
          </form>
        </div>
        <div className="hero-visual">
          <div className="hero-fallback" />
          {showScene && (
            <Suspense fallback={null}>
              <HeroScene key={theme} />
            </Suspense>
          )}
        </div>
      </div>
    </section>
  );
}

export default function HomePage() {
  const { t } = useTranslation();
  const { isActive } = useAuth();
  const home = useQuery({ queryKey: ['home'], queryFn: catalogApi.home });
  const recommendations = useQuery({
    queryKey: ['recommendations', 'HOME', isActive],
    queryFn: () => catalogApi.recommendations('HOME', 8),
    enabled: isActive,
  });

  return (
    <>
      <Hero />
      <div className="container">
        {home.isLoading && (
          <div className="section">
            <CardGridSkeleton count={4} />
          </div>
        )}
        {home.isError && (
          <div className="section">
            <ErrorState error={home.error} onRetry={() => home.refetch()} />
          </div>
        )}
        {home.data && (
          <>
            {home.data.featuredCampaign && (
              <section className="section">
                <div className="feature-band">
                  <div className="stack">
                    <span className="badge badge-accent" style={{ alignSelf: 'flex-start' }}>
                      {home.data.featuredCampaign.audience === 'PLUS' ? t('common.plus') : t('home.popular')}
                    </span>
                    <h2 className="section-title">{home.data.featuredCampaign.name}</h2>
                    <p className="muted">{home.data.featuredCampaign.bannerText}</p>
                    {home.data.featuredCampaign.bonusPoints > 0 && (
                      <p className="reason">
                        <Award size={16} aria-hidden="true" />
                        {t('home.campaignBonus', { points: home.data.featuredCampaign.bonusPoints })}
                      </p>
                    )}
                  </div>
                </div>
                <div className="offer-rail" style={{ marginTop: 16 }}>
                  {home.data.featuredCampaign.offers.map((offer) => (
                    <OfferCard key={offer.id} offer={offer} />
                  ))}
                </div>
              </section>
            )}
            {recommendations.data?.personalized && recommendations.data.items.length > 0 && (
              <section className="section">
                <div className="section-header">
                  <h2 className="section-title">{t('home.forYou')}</h2>
                  <Link to="/for-you" className="section-link">
                    {t('common.seeAll')}
                  </Link>
                </div>
                <RecommendedOffers data={recommendations.data} layout="rail" />
              </section>
            )}
            <OfferRail title={t('home.popular')} offers={home.data.popular} link="/explore" />
            <OfferRail title={t('home.bestValueBundles')} offers={home.data.bestValueBundles} link="/bundles" />
            <OfferRail title={t('home.thisWeek')} offers={home.data.thisWeek} link="/events" />
            <OfferRail title={t('home.endingSoon')} offers={home.data.endingSoon} link="/explore?sort=ENDING_SOON" />
            <section className="section">
              <div className="section-header">
                <h2 className="section-title">{t('home.categories')}</h2>
              </div>
              <div className="category-grid">
                {home.data.categories.map((category) => (
                  <Link key={category.id} to={`/categories/${category.slug}`} className="category-tile">
                    <span className="category-icon">
                      <CategoryIcon iconKey={category.iconKey} />
                    </span>
                    <strong>{category.name}</strong>
                    <span className="caption tertiary">{t('partners.offers', { count: category.offerCount })}</span>
                  </Link>
                ))}
              </div>
            </section>
          </>
        )}
        <section className="section">
          <div className="feature-band">
            <div className="stack">
              <span className="badge badge-plus" style={{ alignSelf: 'flex-start' }}>
                {t('common.plus')}
              </span>
              <h2 className="section-title">{t('home.plusTitle')}</h2>
              <p className="muted">{t('home.plusText')}</p>
            </div>
            <div>
              <Link to="/how-it-works" className="btn btn-primary">
                {t('home.plusCta')}
              </Link>
            </div>
          </div>
        </section>
        <section className="section">
          <div className="section-header">
            <h2 className="section-title">{t('home.howTitle')}</h2>
          </div>
          <div className="steps">
            {[t('home.howStep1'), t('home.howStep2'), t('home.howStep3')].map((text, index) => (
              <div key={index} className="step">
                <span className="step-number">{index + 1}</span>
                <p className="muted">{text}</p>
              </div>
            ))}
          </div>
        </section>
        <section className="section">
          <div className="card stack">
            <h2 className="section-title">{t('home.partnersTitle')}</h2>
            <p className="muted">{t('home.partnersText')}</p>
            <Link to="/partners" className="btn btn-secondary" style={{ alignSelf: 'flex-start' }}>
              {t('nav.partners')}
            </Link>
          </div>
        </section>
      </div>
    </>
  );
}
