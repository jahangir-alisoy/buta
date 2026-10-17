import { useTranslation } from 'react-i18next';
import { OfferBrowser } from './OfferBrowser';

export default function ExplorePage() {
  const { t } = useTranslation();
  return <OfferBrowser title={t('explore.title')} showCategoryChips />;
}
