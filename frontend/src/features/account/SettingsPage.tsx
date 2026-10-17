import { useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Upload } from 'lucide-react';
import { accountApi } from '../../shared/api/endpoints';
import { ApiError } from '../../shared/api/client';
import type { FieldError } from '../../shared/api/types';
import { Field, OtpInput, Segmented } from '../../design-system/components/Controls';
import { errorMessage } from '../../design-system/components/States';
import { useToast } from '../../design-system/components/Toast';
import { useAuth } from '../auth/AuthProvider';
import { useAppearance } from './useAppearance';
import { EmailVerification } from '../auth/EmailVerification';
import type { ThemeChoice } from '../../app/PreferencesProvider';
import { apiUrl } from '../../shared/api/client';
import type { Language } from '../../shared/i18n';

const MAX_AVATAR_BYTES = 2 * 1024 * 1024;

function ProfileSection() {
  const { t } = useTranslation();
  const toast = useToast();
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({ firstName: user?.firstName ?? '', lastName: user?.lastName ?? '', username: user?.username ?? '', phone: user?.phone ?? '' });
  const [errors, setErrors] = useState<FieldError[]>([]);
  const [busy, setBusy] = useState(false);
  const errorFor = (field: string) => errors.find((error) => error.field === field)?.message;

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setErrors([]);
    try {
      const updated = await accountApi.update({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        username: form.username.trim().toLowerCase(),
        phone: form.phone.trim() || undefined,
      });
      setUser(updated);
      toast.show(t('account.saved'));
    } catch (failure) {
      if (failure instanceof ApiError) {
        setErrors(failure.fieldErrors);
      }
      toast.show(errorMessage(failure, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  const set = (key: keyof typeof form) => (event: ChangeEvent<HTMLInputElement>) => setForm((current) => ({ ...current, [key]: event.target.value }));

  return (
    <form className="card stack" onSubmit={submit} noValidate>
      <h2 className="section-title">{t('account.profile')}</h2>
      <div className="form-grid">
        <Field id="settings-first" label={t('auth.firstName')} error={errorFor('firstName')}>
          <input id="settings-first" className="input" autoComplete="given-name" maxLength={50} value={form.firstName} onChange={set('firstName')} />
        </Field>
        <Field id="settings-last" label={t('auth.lastName')} error={errorFor('lastName')}>
          <input id="settings-last" className="input" autoComplete="family-name" maxLength={50} value={form.lastName} onChange={set('lastName')} />
        </Field>
        <Field id="settings-username" label={t('auth.username')} hint={t('auth.usernameHint')} error={errorFor('username')}>
          <input id="settings-username" className="input" autoCapitalize="none" maxLength={30} value={form.username} onChange={set('username')} />
        </Field>
        <Field id="settings-phone" label={`${t('account.phone')} (${t('common.optional')})`} hint={t('account.phoneHint')} error={errorFor('phone')}>
          <input id="settings-phone" className="input" type="tel" inputMode="tel" autoComplete="tel" maxLength={16} value={form.phone} onChange={set('phone')} />
        </Field>
      </div>
      <button type="submit" className="btn btn-primary" disabled={busy} style={{ alignSelf: 'flex-start' }}>
        {busy ? t('common.saving') : t('common.save')}
      </button>
    </form>
  );
}

function AvatarSection() {
  const { t } = useTranslation();
  const toast = useToast();
  const { user, setUser, refresh } = useAuth();
  const input = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  const initials = `${user?.firstName?.[0] ?? ''}${user?.lastName?.[0] ?? ''}`.toUpperCase();

  const upload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) {
      return;
    }
    if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > MAX_AVATAR_BYTES) {
      toast.show(t('account.avatarHint'), 'error');
      return;
    }
    setBusy(true);
    try {
      setUser(await accountApi.uploadAvatar(file));
      toast.show(t('account.saved'));
    } catch (failure) {
      toast.show(errorMessage(failure, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    setBusy(true);
    try {
      await accountApi.removeAvatar();
      await refresh();
    } catch (failure) {
      toast.show(errorMessage(failure, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="card stack">
      <h2 className="section-title">{t('account.avatar')}</h2>
      <div className="row wrap" style={{ gap: 16 }}>
        {user?.avatarUrl ? <img className="avatar avatar-lg" src={apiUrl(user.avatarUrl)} alt="" /> : <span className="avatar avatar-lg">{initials}</span>}
        <div className="stack" style={{ gap: 8 }}>
          <div className="row wrap">
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => input.current?.click()} disabled={busy}>
              <Upload size={16} aria-hidden="true" />
              {t('account.uploadAvatar')}
            </button>
            {user?.avatarUrl && (
              <button type="button" className="btn btn-ghost btn-sm" onClick={remove} disabled={busy}>
                {t('account.removeAvatar')}
              </button>
            )}
          </div>
          <span className="caption muted">{t('account.avatarHint')}</span>
        </div>
        <input ref={input} type="file" accept="image/jpeg,image/png" hidden onChange={upload} />
      </div>
    </section>
  );
}

function EmailSection() {
  const { t } = useTranslation();
  const toast = useToast();
  const { user, setUser } = useAuth();
  const [newEmail, setNewEmail] = useState('');
  const [code, setCode] = useState('');
  const [step, setStep] = useState<'idle' | 'code'>('idle');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const request = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await accountApi.requestEmailChange(newEmail.trim());
      setStep('code');
      toast.show(t('account.codeSent'), 'info');
    } catch (failure) {
      setError(errorMessage(failure, t('errors.generic')));
    } finally {
      setBusy(false);
    }
  };

  const confirm = async () => {
    setBusy(true);
    setError(null);
    try {
      setUser(await accountApi.confirmEmailChange(code));
      setStep('idle');
      setNewEmail('');
      setCode('');
      toast.show(t('account.saved'));
    } catch (failure) {
      setCode('');
      setError(errorMessage(failure, t('errors.generic')));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="card stack">
      <h2 className="section-title">{t('account.email')}</h2>
      <p className="small">
        {user?.email}{' '}
        {user?.emailVerified ? (
          <span className="badge badge-success">{t('account.emailVerified')}</span>
        ) : (
          <span className="badge badge-warning">{t('account.emailNotVerified')}</span>
        )}
      </p>
      {user && !user.emailVerified && <EmailVerification />}
      {step === 'idle' ? (
        <form className="row wrap" onSubmit={request} noValidate>
          <input
            className="input"
            type="email"
            autoComplete="email"
            placeholder={t('account.newEmail')}
            aria-label={t('account.newEmail')}
            value={newEmail}
            onChange={(event) => setNewEmail(event.target.value)}
            style={{ flex: '1 1 220px' }}
          />
          <button type="submit" className="btn btn-secondary" disabled={busy || !newEmail.includes('@')}>
            {t('account.sendCode')}
          </button>
        </form>
      ) : (
        <div className="stack">
          <OtpInput value={code} onChange={setCode} disabled={busy} />
          <div className="row wrap">
            <button type="button" className="btn btn-primary" onClick={confirm} disabled={busy || code.length !== 6}>
              {t('account.confirmChange')}
            </button>
            <button type="button" className="btn btn-ghost" onClick={() => setStep('idle')}>
              {t('common.cancel')}
            </button>
          </div>
        </div>
      )}
      {error && (
        <p className="field-error" role="alert">
          {error}
        </p>
      )}
    </section>
  );
}

function AppearanceSection() {
  const { t } = useTranslation();
  const { theme, language, changeTheme, changeLanguage } = useAppearance();
  return (
    <section className="card stack">
      <h2 className="section-title">{t('account.preferences')}</h2>
      <div className="settings-list">
        <div className="settings-row wrap">
          <span>{t('common.language')}</span>
          <Segmented<Language>
            label={t('common.language')}
            value={language}
            onChange={changeLanguage}
            options={[
              { value: 'az', label: 'Azərbaycanca' },
              { value: 'en', label: 'English' },
            ]}
          />
        </div>
        <div className="settings-row wrap">
          <span>{t('common.theme')}</span>
          <Segmented<ThemeChoice>
            label={t('common.theme')}
            value={theme}
            onChange={changeTheme}
            options={[
              { value: 'SYSTEM', label: t('common.themeSystem') },
              { value: 'LIGHT', label: t('common.themeLight') },
              { value: 'DARK', label: t('common.themeDark') },
            ]}
          />
        </div>
      </div>
    </section>
  );
}

export default function SettingsPage() {
  const { t } = useTranslation();
  return (
    <div className="container page stack" style={{ maxWidth: 820 }}>
      <header className="page-header" style={{ marginBottom: 0 }}>
        <h1 className="page-title">{t('nav.settings')}</h1>
      </header>
      <ProfileSection />
      <AvatarSection />
      <EmailSection />
      <AppearanceSection />
    </div>
  );
}
