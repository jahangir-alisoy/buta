import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MailCheck } from 'lucide-react';
import { authApi } from '../../shared/api/endpoints';
import { OtpInput } from '../../design-system/components/Controls';
import { errorMessage } from '../../design-system/components/States';
import { useToast } from '../../design-system/components/Toast';
import { useAuth } from './AuthProvider';

export function EmailVerification() {
  const { t } = useTranslation();
  const toast = useToast();
  const { user, setUser } = useAuth();
  const [step, setStep] = useState<'idle' | 'code'>('idle');
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!user) {
    return null;
  }

  const send = async () => {
    setBusy(true);
    setError(null);
    try {
      await authApi.requestCode(user.email);
      setStep('code');
    } catch (failure) {
      setError(errorMessage(failure, t('errors.generic')));
    } finally {
      setBusy(false);
    }
  };

  const verify = async (value: string) => {
    if (value.length !== 6) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const result = await authApi.verify(user.email, value);
      setUser(result.user);
      toast.show(t('checkout.verified'));
    } catch (failure) {
      setCode('');
      setError(errorMessage(failure, t('errors.generic')));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="verify-box stack">
      <div className="verify-head">
        <span className="verify-icon">
          <MailCheck size={22} aria-hidden="true" />
        </span>
        <div className="stack" style={{ gap: 4 }}>
          <strong>{t('checkout.verifyTitle')}</strong>
          <span className="small muted">{t('checkout.verifyText', { email: user.email })}</span>
        </div>
      </div>
      {step === 'code' && (
        <OtpInput
          value={code}
          disabled={busy}
          onChange={(value) => {
            setCode(value);
            void verify(value);
          }}
        />
      )}
      {error && (
        <p className="field-error text-center" role="alert">
          {error}
        </p>
      )}
      {step === 'idle' ? (
        <button type="button" className="btn btn-primary btn-block" onClick={send} disabled={busy}>
          {busy ? t('auth.sending') : t('checkout.sendCode')}
        </button>
      ) : (
        <button type="button" className="btn btn-ghost btn-block" onClick={send} disabled={busy}>
          {t('auth.resend')}
        </button>
      )}
    </div>
  );
}
