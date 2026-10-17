import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate, useSearchParams } from 'react-router';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { authApi } from '../../shared/api/endpoints';
import { ApiError } from '../../shared/api/client';
import type { FieldError } from '../../shared/api/types';
import { safeReturnTo } from '../../shared/safeReturnTo';
import { rememberedReferral } from '../../shared/referral';
import { Field } from '../../design-system/components/Controls';
import { errorMessage } from '../../design-system/components/States';
import { useToast } from '../../design-system/components/Toast';
import { PageLoader } from '../../app/PageLoader';
import { useAuth } from './AuthProvider';
import { resumePendingAction } from './resumePendingAction';
import { USERNAME_PATTERN, useUsernameAvailability } from './useUsernameAvailability';

function fieldError(errors: FieldError[], field: string): string | undefined {
  return errors.find((error) => error.field === field)?.message;
}

export default function CompleteProfilePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [params] = useSearchParams();
  const returnTo = safeReturnTo(params.get('returnTo'));
  const { user, loading, setUser } = useAuth();
  const [form, setForm] = useState({ firstName: '', lastName: '', username: '', referralCode: rememberedReferral() });
  const [errors, setErrors] = useState<FieldError[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const availability = useUsernameAvailability(form.username);


  if (loading) {
    return <PageLoader />;
  }
  if (!user) {
    return <Navigate to={`/auth?returnTo=${encodeURIComponent(returnTo)}`} replace />;
  }
  if (user.status === 'ACTIVE' && !busy) {
    return <Navigate to={returnTo} replace />;
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setErrors([]);
    setError(null);
    setBusy(true);
    try {
      const updated = await authApi.completeProfile({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        username: form.username.trim().toLowerCase(),
        referralCode: form.referralCode.trim() || undefined,
      });
      toast.show(t('auth.welcome'));
      const destination = await resumePendingAction(queryClient, toast.show, t, returnTo);
      setUser(updated);
      navigate(destination, { replace: true });
    } catch (failure) {
      if (failure instanceof ApiError) {
        setErrors(failure.fieldErrors);
      }
      setError(errorMessage(failure, t('errors.generic')));
      setBusy(false);
    }
  };

  const set = (key: keyof typeof form) => (value: string) => setForm((current) => ({ ...current, [key]: value }));
  const usernameHint =
    availability === 'available' ? t('auth.usernameAvailable') : availability === 'taken' ? t('auth.usernameTaken') : t('auth.usernameHint');

  return (
    <div className="container auth-shell">
      <form className="auth-card card glass stack" onSubmit={submit} noValidate>
        <div className="stack" style={{ gap: 6 }}>
          <h1 className="section-title">{t('auth.completeTitle')}</h1>
          <p className="muted small">{t('auth.completeSubtitle')}</p>
        </div>
        {error && errors.length === 0 && (
          <p className="alert alert-danger" role="alert">
            {error}
          </p>
        )}
        <Field id="firstName" label={t('auth.firstName')} error={fieldError(errors, 'firstName')}>
          <input id="firstName" className="input" autoComplete="given-name" maxLength={50} required value={form.firstName} onChange={(event) => set('firstName')(event.target.value)} autoFocus />
        </Field>
        <Field id="lastName" label={t('auth.lastName')} error={fieldError(errors, 'lastName')}>
          <input id="lastName" className="input" autoComplete="family-name" maxLength={50} required value={form.lastName} onChange={(event) => set('lastName')(event.target.value)} />
        </Field>
        <Field id="username" label={t('auth.username')} hint={usernameHint} error={fieldError(errors, 'username') ?? (availability === 'taken' ? t('auth.usernameTaken') : undefined)}>
          <input
            id="username"
            className="input"
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            maxLength={30}
            required
            value={form.username}
            onChange={(event) => set('username')(event.target.value.toLowerCase())}
            aria-invalid={availability === 'taken'}
          />
        </Field>
        <Field id="referralCode" label={`${t('auth.referralCode')} (${t('common.optional')})`} error={fieldError(errors, 'referralCode')}>
          <input
            id="referralCode"
            className="input"
            autoCapitalize="characters"
            spellCheck={false}
            maxLength={16}
            value={form.referralCode}
            onChange={(event) => set('referralCode')(event.target.value.toUpperCase())}
          />
        </Field>
        <button
          type="submit"
          className="btn btn-primary btn-lg btn-block"
          disabled={busy || !form.firstName.trim() || !form.lastName.trim() || !USERNAME_PATTERN.test(form.username) || availability === 'taken'}
        >
          {busy ? t('common.saving') : t('auth.finish')}
        </button>
      </form>
    </div>
  );
}
