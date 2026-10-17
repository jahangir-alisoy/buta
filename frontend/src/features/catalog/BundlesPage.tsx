import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { OfferBrowser } from './OfferBrowser';

export default function BundlesPage() {
  const { t } = useTranslation();
  const preset = useMemo(() => ({ offerType: 'BUNDLE' }), []);
  return <OfferBrowser title={t('explore.bundlesTitle')} subtitle={t('explore.bundlesSubtitle')} preset={preset} defaultSort="SAVINGS_DESC" showTypeFilter={false} showCategoryChips />;
}
