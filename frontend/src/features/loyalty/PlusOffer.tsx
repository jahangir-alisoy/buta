import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Check, Crown } from 'lucide-react';
import { accountApi } from '../../shared/api/endpoints';
import { formatDate, formatMoney } from '../../shared/format';
import { Sheet } from '../../design-system/components/Sheet';
import { errorMessage } from '../../design-system/components/States';
import { useToast } from '../../design-system/components/Toast';
import { useAuth } from '../auth/AuthProvider';
import { EmailVerification } from '../auth/EmailVerification';

const BENEFITS = ['points', 'tasks', 'tiers', 'redeem', 'picks', 'exclusive'];

export function PlusOffer({ earnHint }: { earnHint?: string }) {
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { user, refresh } = useAuth();
  const plan = useQuery({ queryKey: ['loyalty', 'plus-plan'], queryFn: accountApi.plusPlan });
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  if (!plan.data || !plan.data.forSale) {
    return null;
  }
  const data = plan.data;
  const price = formatMoney(data.price);
  const isPlus = data.plan === 'PLUS';

  if (isPlus && !data.canPurchase) {
    return null;
  }

  const pay = async () => {
    setBusy(true);
    try {
      const result = await accountApi.buyPlus();
      queryClient.setQueryData(['loyalty', 'plus-plan'], result);
      await refresh();
      void queryClient.invalidateQueries();
      toast.show(isPlus ? t('plus.extended', { date: formatDate(result.endsAt) }) : t('plus.welcome'));
      setOpen(false);
    } catch (failure) {
      toast.show(errorMessage(failure, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      {isPlus ? (
        <div className="plus-extend card">
          <div className="stack" style={{ gap: 4 }}>
            <strong className="row" style={{ gap: 8 }}>
              <Crown size={18} aria-hidden="true" />
              {t('plus.title')}
            </strong>
            {data.endsAt && <span className="small muted">{t('plus.activeUntil', { date: formatDate(data.endsAt) })}</span>}
          </div>
          <button type="button" className="btn btn-secondary" onClick={() => setOpen(true)}>
            {t('plus.extend')}
          </button>
        </div>
      ) : (
        <section className="plus-offer">
          <div className="plus-offer-head">
            <span className="plus-offer-icon">
              <Crown size={22} aria-hidden="true" />
            </span>
            <div className="stack" style={{ gap: 2 }}>
              <h2 className="section-title">{t('plus.title')}</h2>
              <span className="plus-offer-price">{t('plus.perPeriod', { price })}</span>
            </div>
          </div>
          <ul className="plus-benefits">
            {BENEFITS.map((benefit) => (
              <li key={benefit}>
                <Check size={16} aria-hidden="true" />
                {t(`plus.benefits.${benefit}`)}
              </li>
            ))}
          </ul>
          <button type="button" className="btn btn-primary btn-lg btn-block" onClick={() => setOpen(true)}>
            {t('plus.buy')}
          </button>
          <p className="caption muted text-center">{t('plus.periodNote', { days: data.periodDays })}</p>
          {earnHint && <p className="caption tertiary text-center">{earnHint}</p>}
        </section>
      )}
      <Sheet
        open={open}
        title={t('plus.sheetTitle')}
        onClose={() => setOpen(false)}
        footer={
          user?.emailVerified ? (
            <button type="button" className="btn btn-primary btn-lg btn-block" onClick={pay} disabled={busy}>
              {busy ? t('plus.paying') : t('plus.pay', { price })}
            </button>
          ) : undefined
        }
      >
        <div className="stack">
          <dl className="kv-list">
            <div className="kv">
              <dt>{t('plus.plan')}</dt>
              <dd>{t('plus.title')}</dd>
            </div>
            <div className="kv">
              <dt>{t('plus.period')}</dt>
              <dd>{t('plus.periodValue', { days: data.periodDays })}</dd>
            </div>
            <div className="kv kv-total">
              <dt>{t('plus.total')}</dt>
              <dd>{price}</dd>
            </div>
          </dl>
          <p className="caption muted">{t('plus.periodNote', { days: data.periodDays })}</p>
          {user && !user.emailVerified && <EmailVerification />}
        </div>
      </Sheet>
    </>
  );
}
