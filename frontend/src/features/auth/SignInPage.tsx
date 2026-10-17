import { useEffect, useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Mail } from 'lucide-react';
import { authApi, catalogApi } from '../../shared/api/endpoints';
import { ApiError, ensureCsrfToken } from '../../shared/api/client';
import { safeReturnTo } from '../../shared/safeReturnTo';
import { Field, OtpInput } from '../../design-system/components/Controls';
import { errorMessage } from '../../design-system/components/States';
import { useToast } from '../../design-system/components/Toast';
import { BrandMark } from '../../design-system/components/BrandMark';
import { useAuth } from './AuthProvider';
import { resumePendingAction } from './resumePendingAction';

function useCountdown(): [number, (seconds: number) => void] {
  const [until, setUntil] = useState(0);
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    if (until <= now) {
      return;
    }
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [until, now]);
  const remaining = Math.max(0, Math.ceil((until - now) / 1000));
  return [remaining, (seconds: number) => {
    setNow(Date.now());
    setUntil(Date.now() + seconds * 1000);
  }];
}

export default function SignInPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [params] = useSearchParams();
  const returnTo = safeReturnTo(params.get('returnTo'));
  const { user, setUser } = useAuth();
  const config = useQuery({ queryKey: ['config'], queryFn: catalogApi.config, staleTime: Infinity });
  const [step, setStep] = useState<'email' | 'code'>('email');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [cooldown, startCooldown] = useCountdown();

  if (user && !busy) {
    return <Navigate to={user.status === 'ACTIVE' ? returnTo : `/auth/complete-profile?returnTo=${encodeURIComponent(returnTo)}`} replace />;
  }

  const requestCode = async (event?: FormEvent) => {
    event?.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await ensureCsrfToken();
      const result = await authApi.requestCode(email.trim());
      startCooldown(result.resendAfterSeconds);
      setCode('');
      setStep('code');
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 429) {
        const retry = Number(failure.properties.retryAfterSeconds ?? 60);
        startCooldown(Number.isFinite(retry) ? retry : 60);
      }
      setError(errorMessage(failure, t('errors.generic')));
    } finally {
      setBusy(false);
    }
  };

  const verify = async (value: string) => {
    if (value.length !== 6 || busy) {
      return;
    }
    setError(null);
    setBusy(true);
    try {
      const result = await authApi.verify(email.trim(), value);
      queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== 'session' && query.queryKey[0] !== 'config' });
      if (result.nextStep === 'COMPLETE_PROFILE') {
        setUser(result.user);
        navigate(`/auth/complete-profile?returnTo=${encodeURIComponent(returnTo)}`, { replace: true });
        return;
      }
      const destination = await resumePendingAction(queryClient, toast.show, t, returnTo);
      setUser(result.user);
      navigate(destination, { replace: true });
    } catch (failure) {
      setCode('');
      setError(errorMessage(failure, t('errors.generic')));
      setBusy(false);
    }
  };

  const changeCode = (value: string) => {
    setCode(value);
    if (value.length === 6) {
      void verify(value);
    }
  };

  return (
    <div className="container auth-shell">
      <div className="auth-card card glass stack">
        <BrandMark />
        {step === 'email' ? (
          <form className="stack" onSubmit={requestCode} noValidate>
            <div className="stack" style={{ gap: 6 }}>
              <h1 className="section-title">{t('auth.title')}</h1>
              <p className="muted small">{t('auth.subtitle')}</p>
            </div>
            <Field id="email" label={t('auth.email')} error={error ?? undefined}>
              <input
                id="email"
                className="input"
                type="email"
                inputMode="email"
                autoComplete="email"
                autoCapitalize="none"
                spellCheck={false}
                required
                maxLength={254}
                placeholder={t('auth.emailPlaceholder')}
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                aria-invalid={Boolean(error)}
                aria-describedby={error ? 'email-error' : undefined}
                autoFocus
              />
            </Field>
            <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={busy || !email.includes('@') || cooldown > 0}>
              {busy ? t('auth.sending') : cooldown > 0 ? t('auth.resendIn', { seconds: cooldown }) : t('auth.continue')}
            </button>
            <p className="caption tertiary">
              {t('auth.privacyNote')} <Link to="/terms">{t('nav.terms')}</Link> · <Link to="/privacy-policy">{t('nav.privacyPolicy')}</Link>
            </p>
          </form>
        ) : (
          <div className="stack">
            <div className="stack" style={{ gap: 6 }}>
              <h1 className="section-title row" style={{ gap: 8 }}>
                <Mail size={22} aria-hidden="true" />
                {t('auth.codeTitle')}
              </h1>
              <p className="muted small">{t('auth.codeSent', { email: email.trim() })}</p>
            </div>
            <OtpInput value={code} onChange={changeCode} disabled={busy} />
            {error && (
              <p className="field-error" role="alert">
                {error}
              </p>
            )}
            <button type="button" className="btn btn-primary btn-lg btn-block" onClick={() => verify(code)} disabled={busy || code.length !== 6}>
              {busy ? t('auth.verifying') : t('auth.verify')}
            </button>
            <div className="row spread wrap">
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => requestCode()} disabled={busy || cooldown > 0}>
                {cooldown > 0 ? t('auth.resendIn', { seconds: cooldown }) : t('auth.resend')}
              </button>
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => {
                  setStep('email');
                  setError(null);
                }}
              >
                {t('auth.changeEmail')}
              </button>
            </div>
            {config.data?.demoMode && <p className="caption tertiary">{t('auth.devHint')}</p>}
          </div>
        )}
      </div>
    </div>
  );
}
