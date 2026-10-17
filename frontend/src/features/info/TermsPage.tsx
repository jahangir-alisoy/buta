import { useTranslation } from 'react-i18next';
import { InfoPage } from './InfoPage';

export default function TermsPage() {
  const { t } = useTranslation();
  return (
    <InfoPage title={t('info.termsTitle')}>
      <p>{t('info.termsText')}</p>
    </InfoPage>
  );
}
