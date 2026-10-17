import { useTranslation } from 'react-i18next';
import type { Voucher } from '../../shared/api/types';
import { formatDate, formatDateTime } from '../../shared/format';
import { CopyButton } from '../../design-system/components/CopyButton';

export function VoucherCard({ voucher }: { voucher: Voucher }) {
  const { t } = useTranslation();
  const usable = voucher.status === 'ISSUED';
  return (
    <div className="voucher" style={{ opacity: usable ? 1 : 0.65 }}>
      <div className="row spread wrap">
        <span className="caption muted">
          {voucher.productTitle}
          {voucher.partnerName ? ` · ${voucher.partnerName}` : ''}
        </span>
        <span className={`badge ${usable ? 'badge-success' : 'badge-soft'}`}>{t(`orders.voucherStatuses.${voucher.status}`, { defaultValue: voucher.status })}</span>
      </div>
      <div className="row spread wrap">
        <span className="voucher-code" aria-label={t('orders.voucherCode')}>
          {voucher.code}
        </span>
        {usable && <CopyButton value={voucher.code} label={t('orders.voucherCode')} />}
      </div>
      <span className="caption muted">
        {voucher.eventStartsAt ? formatDateTime(voucher.eventStartsAt) : t('orders.validUntil', { date: formatDate(voucher.validUntil) })}
        {voucher.venueName ? ` · ${voucher.venueName}` : ''}
      </span>
      {voucher.redemptionInstructions && (
        <p className="caption">
          {voucher.redemptionMethod && <strong>{t(`redemptionMethods.${voucher.redemptionMethod}`, { defaultValue: voucher.redemptionMethod })}. </strong>}
          {voucher.redemptionInstructions}
        </p>
      )}
    </div>
  );
}
