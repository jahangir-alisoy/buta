import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { CircleCheck } from 'lucide-react';
import { errorMessage } from '../../../design-system/components/States';
import { adminApi } from '../api';
import { AdminHeader, FormField } from '../forms';
import type { RedemptionResult } from '../types';

export default function AdminRedeemPage() {
  const { t } = useTranslation();
  const [code, setCode] = useState('');
  const [result, setResult] = useState<RedemptionResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      setResult(await adminApi.redeem(code.trim().toUpperCase()));
    } catch (failure) {
      setError(errorMessage(failure, t('errors.generic')));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <AdminHeader title={t('admin.redeem.title')} />
      <form className="card stack" style={{ maxWidth: 520 }} onSubmit={submit} noValidate>
        <p className="small muted">{t('admin.redeem.text')}</p>
        <FormField id="redeem-code" label={t('orders.voucherCode')}>
          <input
            id="redeem-code"
            className="input voucher-code"
            autoCapitalize="characters"
            autoComplete="off"
            spellCheck={false}
            maxLength={20}
            value={code}
            onChange={(event) => setCode(event.target.value.toUpperCase())}
          />
        </FormField>
        <button type="submit" className="btn btn-primary" disabled={busy || code.trim().length < 6}>
          {t('admin.redeem.submit')}
        </button>
        {result && (
          <div className={`alert ${result.changed ? 'alert-success' : 'alert-info'}`} role="status">
            <CircleCheck size={18} aria-hidden="true" />
            <div>
              <p>{t('admin.redeem.result', { code: result.code, order: result.orderNumber, status: t(`orders.voucherStatuses.${result.status}`, { defaultValue: result.status }) })}</p>
              {!result.changed && <p className="caption">{t('admin.redeem.unchanged')}</p>}
            </div>
          </div>
        )}
        {error && (
          <p className="alert alert-danger" role="alert">
            {error}
          </p>
        )}
      </form>
    </>
  );
}
