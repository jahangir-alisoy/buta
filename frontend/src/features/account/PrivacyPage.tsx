import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { accountApi, catalogApi } from '../../shared/api/endpoints';
import type { Preferences } from '../../shared/api/types';
import { Field, Switch } from '../../design-system/components/Controls';
import { ErrorState, errorMessage } from '../../design-system/components/States';
import { ConfirmSheet } from '../../design-system/components/ConfirmSheet';
import { useToast } from '../../design-system/components/Toast';
import { PageLoader } from '../../app/PageLoader';
import { useAuth } from '../auth/AuthProvider';
import { preferencesUpdate } from './preferencesUpdate';

const MAX_CATEGORIES = 9;

export default function PrivacyPage() {
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { refresh } = useAuth();
  const preferences = useQuery({ queryKey: ['preferences'], queryFn: accountApi.preferences, staleTime: 0 });
  const categories = useQuery({ queryKey: ['categories'], queryFn: catalogApi.categories });
  const [draft, setDraft] = useState<Preferences | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmReset, setConfirmReset] = useState(false);

  useEffect(() => {
    if (preferences.data) {
      setDraft(preferences.data);
    }
  }, [preferences.data]);

  if (preferences.isLoading || !draft) {
    return preferences.isError ? (
      <div className="container page">
        <ErrorState error={preferences.error} onRetry={() => preferences.refetch()} />
      </div>
    ) : (
      <PageLoader />
    );
  }

  const save = async (next: Preferences, personalizationChanged = false) => {
    setBusy(true);
    try {
      const saved = await accountApi.updatePreferences(
        preferencesUpdate(next, personalizationChanged ? { personalizationEnabled: next.personalizationEnabled } : {}),
      );
      queryClient.setQueryData(['preferences'], saved);
      setDraft(saved);
      void queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      if (personalizationChanged) {
        await refresh();
      }
      toast.show(t('account.saved'));
    } catch (failure) {
      toast.show(errorMessage(failure, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  const toggleCategory = (id: string) => {
    const selected = draft.preferredCategoryIds.includes(id);
    if (!selected && draft.preferredCategoryIds.length >= MAX_CATEGORIES) {
      return;
    }
    setDraft({ ...draft, preferredCategoryIds: selected ? draft.preferredCategoryIds.filter((item) => item !== id) : [...draft.preferredCategoryIds, id] });
  };

  const budget = (value: string) => (value === '' ? undefined : Math.max(0, Number(value)));

  const reset = async () => {
    setBusy(true);
    try {
      await accountApi.resetPersonalization();
      void queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      toast.show(t('account.resetDone'));
    } catch (failure) {
      toast.show(errorMessage(failure, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
      setConfirmReset(false);
    }
  };

  return (
    <div className="container page stack" style={{ maxWidth: 820 }}>
      <header className="page-header" style={{ marginBottom: 0 }}>
        <h1 className="page-title">{t('account.privacyTitle')}</h1>
        <p className="muted small">{t('account.dataInfo')}</p>
      </header>
      <section className="card stack">
        <div className="settings-row" style={{ padding: 0 }}>
          <div className="stack" style={{ gap: 4 }}>
            <strong>{t('account.personalization')}</strong>
            <span className="small muted">{t('account.personalizationText')}</span>
          </div>
          <Switch
            checked={draft.personalizationEnabled}
            label={t('account.personalization')}
            disabled={busy}
            onChange={(value) => save({ ...draft, personalizationEnabled: value }, true)}
          />
        </div>
      </section>
      <section className="card stack">
        <h2 className="section-title">{t('account.preferredCategories')}</h2>
        <div className="row wrap" style={{ gap: 8 }}>
          {categories.data?.map((category) => (
            <button
              key={category.id}
              type="button"
              className={`chip${draft.preferredCategoryIds.includes(category.id) ? ' is-selected' : ''}`}
              aria-pressed={draft.preferredCategoryIds.includes(category.id)}
              onClick={() => toggleCategory(category.id)}
            >
              {category.name}
            </button>
          ))}
        </div>
        <div className="form-grid">
          <Field id="budget-min" label={`${t('account.budget')} · ${t('explore.min')}`}>
            <input id="budget-min" className="input" type="number" inputMode="decimal" min={0} max={10000} value={draft.budgetMin ?? ''} onChange={(event) => setDraft({ ...draft, budgetMin: budget(event.target.value) })} />
          </Field>
          <Field id="budget-max" label={`${t('account.budget')} · ${t('explore.max')}`}>
            <input id="budget-max" className="input" type="number" inputMode="decimal" min={0} max={10000} value={draft.budgetMax ?? ''} onChange={(event) => setDraft({ ...draft, budgetMax: budget(event.target.value) })} />
          </Field>
          <Field id="city" label={t('account.city')}>
            <input id="city" className="input" maxLength={60} autoComplete="address-level2" value={draft.city ?? ''} onChange={(event) => setDraft({ ...draft, city: event.target.value || undefined })} />
          </Field>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => save(draft)} disabled={busy} style={{ alignSelf: 'flex-start' }}>
          {busy ? t('common.saving') : t('common.save')}
        </button>
      </section>
      <section className="card stack">
        <h2 className="section-title">{t('account.resetData')}</h2>
        <p className="small muted">{t('account.resetText')}</p>
        <button type="button" className="btn btn-danger" onClick={() => setConfirmReset(true)} style={{ alignSelf: 'flex-start' }}>
          {t('account.resetData')}
        </button>
      </section>
      <ConfirmSheet
        open={confirmReset}
        title={t('account.resetConfirm')}
        confirmLabel={t('account.resetData')}
        danger
        busy={busy}
        onClose={() => setConfirmReset(false)}
        onConfirm={reset}
      >
        {t('account.resetText')}
      </ConfirmSheet>
    </div>
  );
}
