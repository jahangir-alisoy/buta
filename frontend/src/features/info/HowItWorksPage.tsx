import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Award, ScrollText, Sparkles, Ticket } from 'lucide-react';
import { InfoPage } from './InfoPage';

export default function HowItWorksPage() {
  const { t } = useTranslation();
  const items = [
    { icon: <ScrollText size={20} />, title: t('info.how1Title'), text: t('info.how1Text') },
    { icon: <Ticket size={20} />, title: t('info.how2Title'), text: t('info.how2Text') },
    { icon: <Award size={20} />, title: t('info.how3Title'), text: t('info.how3Text') },
    { icon: <Sparkles size={20} />, title: t('info.how4Title'), text: t('info.how4Text') },
  ];
  return (
    <InfoPage title={t('info.howTitle')}>
      {items.map((item) => (
        <div key={item.title} className="card row" style={{ alignItems: 'flex-start' }}>
          <span className="category-icon">{item.icon}</span>
          <div className="stack" style={{ gap: 4 }}>
            <h2 className="section-title" style={{ color: 'var(--color-text)' }}>
              {item.title}
            </h2>
            <p>{item.text}</p>
          </div>
        </div>
      ))}
      <p>{t('home.plusText')}</p>
      <Link to="/explore" className="btn btn-primary" style={{ alignSelf: 'flex-start' }}>
        {t('cart.browse')}
      </Link>
    </InfoPage>
  );
}
