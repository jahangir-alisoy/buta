import { useTranslation } from 'react-i18next';
import { InfoPage } from './InfoPage';

export default function PrivacyPolicyPage() {
  const { t } = useTranslation();
  return (
    <InfoPage title={t('info.privacyTitle')}>
      <p>{t('info.privacyText')}</p>
      <p>{t('account.dataInfo')}</p>
    </InfoPage>
  );
}
