import { useTranslation } from 'react-i18next';
import { InfoPage } from './InfoPage';

export default function AboutPage() {
  const { t } = useTranslation();
  return (
    <InfoPage title={t('info.aboutTitle')}>
      <p>{t('info.aboutText1')}</p>
      <p>{t('info.aboutText2')}</p>
    </InfoPage>
  );
}
