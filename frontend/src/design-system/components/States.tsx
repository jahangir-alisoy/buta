import type { ReactNode } from 'react';
import { CircleAlert, Search } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { ApiError } from '../../shared/api/client';

export function Skeleton({ height = 16, width = '100%', radius }: { height?: number | string; width?: number | string; radius?: number }) {
  return <div className="skeleton" style={{ height, width, borderRadius: radius }} aria-hidden="true" />;
}

export function CardGridSkeleton({ count = 6 }: { count?: number }) {
  return (
    <div className="offer-grid" aria-busy="true">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="offer-card">
          <div className="skeleton" style={{ aspectRatio: '16 / 10', borderRadius: 0 }} />
          <div className="offer-body">
            <Skeleton height={12} width="40%" />
            <Skeleton height={18} width="80%" />
            <Skeleton height={14} width="60%" />
          </div>
        </div>
      ))}
    </div>
  );
}

interface EmptyStateProps {
  title: string;
  text?: string;
  action?: ReactNode;
  icon?: ReactNode;
}

export function EmptyState({ title, text, action, icon }: EmptyStateProps) {
  return (
    <div className="state">
      <div className="state-icon">{icon ?? <Search size={24} />}</div>
      <h3 className="section-title">{title}</h3>
      {text && <p className="muted small">{text}</p>}
      {action}
    </div>
  );
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const { t } = useTranslation();
  const apiError = error instanceof ApiError ? error : null;
  return (
    <div className="state" role="alert">
      <div className="state-icon">
        <CircleAlert size={24} />
      </div>
      <h3 className="section-title">{t('common.errorTitle')}</h3>
      <p className="muted small">{apiError ? apiError.message : t('common.errorText')}</p>
      {apiError?.requestId && <p className="caption tertiary">{t('common.requestId', { id: apiError.requestId })}</p>}
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          {t('common.retry')}
        </button>
      )}
    </div>
  );
}

export function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return fallback;
}
