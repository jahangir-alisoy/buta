import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Compass } from 'lucide-react';
import { EmptyState } from '../../design-system/components/States';

export default function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <div className="container page">
      <EmptyState
        icon={<Compass size={24} />}
        title={t('common.notFoundTitle')}
        text={t('common.notFoundText')}
        action={
          <Link to="/" className="btn btn-primary">
            {t('common.goHome')}
          </Link>
        }
      />
    </div>
  );
}
