import { useTranslation } from 'react-i18next';
import { InfoPage } from './InfoPage';
import { legalDocument } from './legal';

export default function TermsPage() {
  const { t } = useTranslation();
  const document = legalDocument('terms', t);
  return (
    <InfoPage title={document.title}>
      {document.paragraphs.map((paragraph) => (
        <p key={paragraph}>{paragraph}</p>
      ))}
    </InfoPage>
  );
}
