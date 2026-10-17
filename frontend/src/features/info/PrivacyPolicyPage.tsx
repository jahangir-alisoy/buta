import { useTranslation } from 'react-i18next';
import { InfoPage } from './InfoPage';
import { legalDocument } from './legal';

export default function PrivacyPolicyPage() {
  const { t } = useTranslation();
  const document = legalDocument('privacy', t);
  return (
    <InfoPage title={document.title}>
      {document.paragraphs.map((paragraph) => (
        <p key={paragraph}>{paragraph}</p>
      ))}
    </InfoPage>
  );
}
