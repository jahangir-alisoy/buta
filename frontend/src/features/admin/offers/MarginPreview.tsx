import { useTranslation } from 'react-i18next';
import { Check, X } from 'lucide-react';
import { formatMoney } from '../../../shared/format';
import type { PriceBreakdown } from '../types';

export function MarginPreview({ breakdown }: { breakdown: PriceBreakdown | undefined }) {
  const { t } = useTranslation();
  if (!breakdown) {
    return <p className="small muted">{t('admin.offers.previewHint')}</p>;
  }
  const rows: [string, string][] = [
    [t('admin.fields.sellingPrice'), formatMoney(breakdown.sellingPrice)],
    [t('admin.offers.referenceRetail'), formatMoney(breakdown.referenceRetail)],
    [t('admin.offers.savings'), `${formatMoney(breakdown.savings)} (${breakdown.savingsPercent}%)`],
    [t('admin.offers.partnerCost'), `−${formatMoney(breakdown.partnerCostTotal)}`],
    [t('admin.offers.paymentFee'), `−${formatMoney(breakdown.paymentFee)}`],
    [t('admin.offers.loyaltyCost'), `−${formatMoney(breakdown.expectedLoyaltyCost)}`],
    [t('admin.offers.otherCost'), `−${formatMoney(breakdown.otherVariableCost)}`],
    [t('admin.offers.maxPoints'), String(breakdown.maxPointsEarned)],
  ];
  return (
    <div className="stack" style={{ gap: 10 }}>
      <dl className="stack" style={{ gap: 6, margin: 0 }}>
        {rows.map(([label, value]) => (
          <div key={label} className="kv">
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
        <div className="kv" style={{ fontSize: 17 }}>
          <dt>{t('admin.offers.contributionMargin')}</dt>
          <dd style={{ color: breakdown.sellable ? 'var(--color-success)' : 'var(--color-danger)' }}>
            {formatMoney(breakdown.contributionMargin)} ({breakdown.marginPercent}%)
          </dd>
        </div>
      </dl>
      <ul className="stack" style={{ listStyle: 'none', margin: 0, padding: 0, gap: 4 }}>
        {breakdown.rules.map((rule) => (
          <li key={rule.code} className="row small" style={{ gap: 6, color: rule.passed ? 'var(--color-success)' : 'var(--color-danger)' }}>
            {rule.passed ? <Check size={16} aria-hidden="true" /> : <X size={16} aria-hidden="true" />}
            {t(`admin.offers.rules.${rule.code}`, { defaultValue: rule.code })} ({rule.threshold})
          </li>
        ))}
      </ul>
      <span className={`badge ${breakdown.sellable ? 'badge-success' : 'badge-danger'}`} style={{ alignSelf: 'flex-start' }}>
        {breakdown.sellable ? t('admin.offers.sellable') : t('admin.offers.notSellable')}
      </span>
    </div>
  );
}
