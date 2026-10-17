import { useTranslation } from 'react-i18next';

const TONES: Record<string, string> = {
  PENDING_PAYMENT: 'badge-warning',
  FULFILLED: 'badge-success',
  COMPLETED: 'badge-success',
  CANCELLED: 'badge-soft',
  EXPIRED: 'badge-soft',
  PARTIALLY_REFUNDED: 'badge-warning',
  REFUNDED: 'badge-soft',
};

export function OrderStatusBadge({ status }: { status: string }) {
  const { t } = useTranslation();
  return <span className={`badge ${TONES[status] ?? 'badge-soft'}`}>{t(`orders.statuses.${status}`, { defaultValue: status })}</span>;
}
