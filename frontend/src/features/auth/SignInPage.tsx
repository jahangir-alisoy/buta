import { useEffect, useState, type FormEvent } from 'react';
import { Navigate, useNavigate, useSearchParams } from 'react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Mail, Shield, User } from 'lucide-react';
import { authApi, catalogApi } from '../../shared/api/endpoints';
import { ApiError, ensureCsrfToken } from '../../shared/api/client';
import type { FieldError, SignInResponse } from '../../shared/api/types';
import { safeReturnTo } from '../../shared/safeReturnTo';
import { rememberedReferral } from '../../shared/referral';
import { Field, OtpInput, Segmented } from '../../design-system/components/Controls';
import { errorMessage } from '../../design-system/components/States';
import { useToast } from '../../design-system/components/Toast';
import { BrandMark } from '../../design-system/components/BrandMark';
import { useAuth } from './AuthProvider';
import { resumePendingAction } from './resumePendingAction';
import { USERNAME_PATTERN, useUsernameAvailability } from './useUsernameAvailability';
import { ConsentCheckbox, ConsentNote } from './LegalConsent';
import { AvailabilityMark } from './AvailabilityMark';

type Tab = 'signIn' | 'register';

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
  return [
    remaining,
    (seconds: number) => {
      setNow(Date.now());
      setUntil(Date.now() + seconds * 1000);
    },
  ];
}

function fieldError(errors: FieldError[], field: string): string | undefined {
  return errors.find((error) => error.field === field)?.message;
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
  const [tab, setTab] = useState<Tab>(params.get('ref') ? 'register' : 'signIn');
  const [step, setStep] = useState<'email' | 'code'>('email');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [profile, setProfile] = useState({ firstName: '', lastName: '', username: '', referralCode: rememberedReferral(params.get('ref')) });
  const [errors, setErrors] = useState<FieldError[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [agreed, setAgreed] = useState(false);
  const [cooldown, startCooldown] = useCountdown();
  const availability = useUsernameAvailability(profile.username);

  if (user && !busy) {
    return <Navigate to={user.status === 'ACTIVE' ? returnTo : `/auth/complete-profile?returnTo=${encodeURIComponent(returnTo)}`} replace />;
  }

  const finish = async (result: SignInResponse, fallback: string) => {
    queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== 'session' && query.queryKey[0] !== 'config' });
    if (result.nextStep === 'COMPLETE_PROFILE') {
      setUser(result.user);
      navigate(`/auth/complete-profile?returnTo=${encodeURIComponent(returnTo)}`, { replace: true });
      return;
    }
    const destination = await resumePendingAction(queryClient, toast.show, t, fallback);
    setUser(result.user);
    navigate(destination, { replace: true });
  };

  const run = async (action: () => Promise<void>) => {
    setError(null);
    setErrors([]);
    setBusy(true);
    try {
      await ensureCsrfToken();
      await action();
    } catch (failure) {
      if (failure instanceof ApiError) {
        setErrors(failure.fieldErrors);
        if (failure.status === 429) {
          const retry = Number(failure.properties.retryAfterSeconds ?? 60);
          startCooldown(Number.isFinite(retry) ? retry : 60);
        }
      }
      setError(errorMessage(failure, t('errors.generic')));
      setBusy(false);
    }
  };

  const requestCode = (event?: FormEvent) => {
    event?.preventDefault();
    return run(async () => {
      const result = await authApi.requestCode(email.trim());
      startCooldown(result.resendAfterSeconds);
      setCode('');
      setStep('code');
      setBusy(false);
    });
  };

  const verify = (value: string) => {
    if (value.length !== 6 || busy) {
      return;
    }
    void run(async () => {
      try {
        await finish(await authApi.verify(email.trim(), value), returnTo);
      } catch (failure) {
        setCode('');
        throw failure;
      }
    });
  };

  const register = (event: FormEvent) => {
    event.preventDefault();
    void run(async () => {
      const result = await authApi.register({
        email: email.trim(),
        firstName: profile.firstName.trim(),
        lastName: profile.lastName.trim(),
        username: profile.username.trim().toLowerCase(),
        referralCode: profile.referralCode.trim() || undefined,
      });
      toast.show(t('auth.welcome'));
      await finish(result, returnTo);
    });
  };

  const guest = (kind: 'CUSTOMER' | 'ADMIN') =>
    run(async () => {
      await finish(await authApi.guest(kind), kind === 'ADMIN' && returnTo === '/' ? '/admin' : returnTo);
    });

  const changeCode = (value: string) => {
    setCode(value);
    if (value.length === 6) {
      verify(value);
    }
  };

  const setField = (key: keyof typeof profile) => (value: string) => setProfile((current) => ({ ...current, [key]: value }));
  const emailValid = email.includes('@') && email.trim().length > 3;
  const canRegister =
    agreed && emailValid && profile.firstName.trim() && profile.lastName.trim() && USERNAME_PATTERN.test(profile.username) && availability !== 'taken';

  return (
    <div className="container auth-shell">
      <div className="auth-card card glass">
        <div className="auth-head">
          <BrandMark />
          <h1 className="section-title">{step === 'code' ? t('auth.codeTitle') : t('auth.title')}</h1>
          <p className="muted small">
            {step === 'code' ? t('auth.codeSent', { email: email.trim() }) : tab === 'signIn' ? t('auth.subtitle') : t('auth.registerSubtitle')}
          </p>
        </div>

        {step === 'code' ? (
          <div className="stack">
            <OtpInput value={code} onChange={changeCode} disabled={busy} />
            {error && (
              <p className="field-error text-center" role="alert">
                {error}
              </p>
            )}
            <button type="button" className="btn btn-primary btn-lg btn-block" onClick={() => verify(code)} disabled={busy || code.length !== 6}>
              <Mail size={18} aria-hidden="true" />
              {busy ? t('auth.verifying') : t('auth.verify')}
            </button>
            <div className="auth-actions">
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
          </div>
        ) : (
          <div className="stack">
            <Segmented<Tab>
              label={t('auth.title')}
              value={tab}
              onChange={(value) => {
                setTab(value);
                setError(null);
                setErrors([]);
              }}
              options={[
                { value: 'signIn', label: t('auth.tabSignIn') },
                { value: 'register', label: t('auth.tabRegister') },
              ]}
            />
            <form className="stack" onSubmit={tab === 'signIn' ? requestCode : register} noValidate>
              <Field id="email" label={t('auth.email')} error={fieldError(errors, 'email')}>
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
                />
              </Field>
              {tab === 'register' && (
                <>
                  <div className="field-pair">
                    <Field id="firstName" label={t('auth.firstName')} error={fieldError(errors, 'firstName')}>
                      <input id="firstName" className="input" autoComplete="given-name" maxLength={50} value={profile.firstName} onChange={(event) => setField('firstName')(event.target.value)} />
                    </Field>
                    <Field id="lastName" label={t('auth.lastName')} error={fieldError(errors, 'lastName')}>
                      <input id="lastName" className="input" autoComplete="family-name" maxLength={50} value={profile.lastName} onChange={(event) => setField('lastName')(event.target.value)} />
                    </Field>
                  </div>
                  <Field id="username" label={t('auth.username')} error={fieldError(errors, 'username')}>
                    <div className="input-with-mark">
                      <input
                        id="username"
                        className="input"
                        autoComplete="username"
                        autoCapitalize="none"
                        spellCheck={false}
                        maxLength={30}
                        value={profile.username}
                        onChange={(event) => setField('username')(event.target.value.toLowerCase())}
                        aria-invalid={availability === 'taken'}
                      />
                      <AvailabilityMark availability={availability} />
                    </div>
                  </Field>
                  <Field id="referralCode" label={`${t('auth.referralCode')} (${t('common.optional')})`} error={fieldError(errors, 'referralCode')}>
                    <input
                      id="referralCode"
                      className="input"
                      autoCapitalize="characters"
                      spellCheck={false}
                      maxLength={12}
                      value={profile.referralCode}
                      onChange={(event) => setField('referralCode')(event.target.value.toUpperCase())}
                    />
                  </Field>
                  <ConsentCheckbox checked={agreed} onChange={setAgreed} />
                </>
              )}
              {error && errors.length === 0 && (
                <p className="field-error text-center" role="alert">
                  {error}
                </p>
              )}
              {tab === 'signIn' ? (
                <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={busy || !emailValid || cooldown > 0}>
                  {busy ? t('auth.sending') : cooldown > 0 ? t('auth.resendIn', { seconds: cooldown }) : t('auth.continue')}
                </button>
              ) : (
                <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={busy || !canRegister}>
                  {busy ? t('auth.creating') : t('auth.createAccount')}
                </button>
              )}
            </form>

            {config.data?.demoMode && (
              <div className="auth-quick">
                <div className="auth-divider">
                  <span>{t('auth.quickTitle')}</span>
                </div>
                <div className="auth-quick-actions">
                  <button type="button" className="btn btn-secondary" onClick={() => guest('CUSTOMER')} disabled={busy}>
                    <User size={18} aria-hidden="true" />
                    {t('auth.asCustomer')}
                  </button>
                  <button type="button" className="btn btn-secondary" onClick={() => guest('ADMIN')} disabled={busy}>
                    <Shield size={18} aria-hidden="true" />
                    {t('auth.asAdmin')}
                  </button>
                </div>
              </div>
            )}

            {tab === 'signIn' && <ConsentNote />}
          </div>
        )}
      </div>
    </div>
  );
}
