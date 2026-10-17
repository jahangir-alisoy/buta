import { useState } from 'react';
import { Link } from 'react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Sparkles } from 'lucide-react';
import { accountApi, catalogApi } from '../../shared/api/endpoints';
import { CardGridSkeleton, EmptyState, ErrorState, errorMessage } from '../../design-system/components/States';
import { useToast } from '../../design-system/components/Toast';
import { useAuth } from '../auth/AuthProvider';
import { preferencesUpdate } from '../account/preferencesUpdate';
import { RecommendedOffers } from '../catalog/OfferCollection';

function ConsentPrompt() {
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { refresh } = useAuth();
  const [busy, setBusy] = useState(false);

  const decide = async (enabled: boolean) => {
    setBusy(true);
    try {
      const current = await accountApi.preferences();
      await accountApi.updatePreferences(preferencesUpdate(current, { personalizationEnabled: enabled }));
      await refresh();
      void queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    } catch (failure) {
      toast.show(errorMessage(failure, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="feature-band" style={{ marginBottom: 24 }}>
      <div className="stack">
        <h2 className="section-title">{t('forYou.consentTitle')}</h2>
        <p className="muted small">{t('forYou.consentText')}</p>
      </div>
      <div className="row wrap">
        <button type="button" className="btn btn-primary" onClick={() => decide(true)} disabled={busy}>
          {t('forYou.enable')}
        </button>
        <button type="button" className="btn btn-ghost" onClick={() => decide(false)} disabled={busy}>
          {t('forYou.notNow')}
        </button>
      </div>
    </section>
  );
}

export default function ForYouPage() {
  const { t } = useTranslation();
  const { user, isActive, isPlus } = useAuth();
  const recommendations = useQuery({
    queryKey: ['recommendations', 'FOR_YOU', user?.id ?? 'anonymous'],
    queryFn: () => catalogApi.recommendations('FOR_YOU', 24),
  });

  return (
    <div className="container page">
      <header className="page-header">
        <h1 className="page-title">{t('forYou.title')}</h1>
        <p className="muted">{t('forYou.subtitle')}</p>
      </header>
      {!user && (
        <div className="alert alert-info" style={{ marginBottom: 24 }}>
          <Sparkles size={18} aria-hidden="true" />
          <span>
            {t('forYou.plusText')}{' '}
            <Link to="/auth?returnTo=%2Ffor-you" className="section-link">
              {t('common.signIn')}
            </Link>
          </span>
        </div>
      )}
      {isActive && !isPlus && (
        <div className="alert alert-info" style={{ marginBottom: 24 }}>
          <Sparkles size={18} aria-hidden="true" />
          <span>
            {t('forYou.plusText')}{' '}
            <Link to="/rewards" className="section-link">
              {t('offer.getPlus')}
            </Link>
          </span>
        </div>
      )}
      {isActive && isPlus && user && !user.personalizationDecided && <ConsentPrompt />}
      {recommendations.data && !recommendations.data.personalized && recommendations.data.items.length > 0 && (
        <p className="small muted" style={{ marginBottom: 16 }}>
          {t('forYou.popularFallback')}
        </p>
      )}
      {recommendations.isLoading && <CardGridSkeleton />}
      {recommendations.isError && <ErrorState error={recommendations.error} onRetry={() => recommendations.refetch()} />}
      {recommendations.data && recommendations.data.items.length === 0 && <EmptyState title={t('explore.empty')} />}
      {recommendations.data && recommendations.data.items.length > 0 && <RecommendedOffers data={recommendations.data} layout="grid" />}
    </div>
  );
}
