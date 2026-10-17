import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { OfferBrowser } from './OfferBrowser';

export default function EventsPage() {
  const { t } = useTranslation();
  const preset = useMemo(() => {
    const now = new Date();
    now.setMinutes(0, 0, 0);
    return { eventFrom: now.toISOString() };
  }, []);
  return <OfferBrowser title={t('explore.eventsTitle')} subtitle={t('explore.eventsSubtitle')} preset={preset} defaultSort="EVENT_DATE" showCategoryChips />;
}
