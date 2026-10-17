import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ApiError } from '../../../shared/api/client';
import { CardGridSkeleton, ErrorState, errorMessage } from '../../../design-system/components/States';
import { Segmented } from '../../../design-system/components/Controls';
import { useToast } from '../../../design-system/components/Toast';
import { adminApi } from '../api';
import { AdminHeader } from '../forms';
import type { ImpactPreview, SettingsKey } from '../types';

type Settings = Record<string, unknown>;

function humanize(key: string): string {
  const spaced = key.replace(/([a-z0-9])([A-Z])/g, '$1 $2');
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

function setAt(source: Settings, path: string[], value: unknown): Settings {
  const [head, ...rest] = path;
  return { ...source, [head]: rest.length === 0 ? value : setAt((source[head] ?? {}) as Settings, rest, value) };
}

function JsonArrayField({ id, value, onChange }: { id: string; value: unknown[]; onChange: (value: unknown[]) => void }) {
  const [text, setText] = useState(() => JSON.stringify(value, null, 2));
  const [invalid, setInvalid] = useState(false);
  return (
    <textarea
      id={id}
      className="textarea"
      rows={8}
      spellCheck={false}
      style={{ fontFamily: 'ui-monospace, Menlo, monospace', fontSize: 13 }}
      aria-invalid={invalid}
      value={text}
      onChange={(event) => {
        setText(event.target.value);
        try {
          const parsed = JSON.parse(event.target.value);
          if (Array.isArray(parsed)) {
            setInvalid(false);
            onChange(parsed);
            return;
          }
          setInvalid(true);
        } catch {
          setInvalid(true);
        }
      }}
    />
  );
}

function SettingsFields({ value, path, onChange }: { value: Settings; path: string[]; onChange: (path: string[], value: unknown) => void }) {
  return (
    <div className="form-grid">
      {Object.entries(value).map(([key, entry]) => {
        const id = ['setting', ...path, key].join('-');
        const label = humanize(key);
        if (Array.isArray(entry)) {
          return (
            <div key={key} className="field span-all">
              <label htmlFor={id}>{label}</label>
              <JsonArrayField id={id} value={entry} onChange={(next) => onChange([...path, key], next)} />
            </div>
          );
        }
        if (entry !== null && typeof entry === 'object') {
          return (
            <fieldset key={key} className="span-all card" style={{ margin: 0 }}>
              <legend className="small" style={{ fontWeight: 700, padding: '0 6px' }}>
                {label}
              </legend>
              <SettingsFields value={entry as Settings} path={[...path, key]} onChange={onChange} />
            </fieldset>
          );
        }
        if (typeof entry === 'boolean') {
          return (
            <label key={key} className="checkbox">
              <input type="checkbox" checked={entry} onChange={(event) => onChange([...path, key], event.target.checked)} />
              {label}
            </label>
          );
        }
        return (
          <div key={key} className="field">
            <label htmlFor={id}>{label}</label>
            <input
              id={id}
              className="input"
              type={typeof entry === 'number' ? 'number' : 'text'}
              step="any"
              value={entry === null || entry === undefined ? '' : String(entry)}
              onChange={(event) => onChange([...path, key], typeof entry === 'number' ? Number(event.target.value) : event.target.value)}
            />
          </div>
        );
      })}
    </div>
  );
}

function SettingsEditor({ settingsKey }: { settingsKey: SettingsKey }) {
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const settings = useQuery({ queryKey: ['admin', 'settings', settingsKey], queryFn: () => adminApi.settings(settingsKey), staleTime: 0 });
  const [draft, setDraft] = useState<Settings | null>(null);
  const [impact, setImpact] = useState<ImpactPreview | null>(null);
  const [errors, setErrors] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (settings.data) {
      setDraft(settings.data);
      setImpact(null);
    }
  }, [settings.data]);

  if (settings.isLoading || (!draft && !settings.isError)) {
    return <CardGridSkeleton count={1} />;
  }
  if (settings.isError || !draft) {
    return <ErrorState error={settings.error} onRetry={() => settings.refetch()} />;
  }

  const handle = (error: unknown) => {
    setErrors(error instanceof ApiError ? error.fieldErrors.map((item) => `${item.field}: ${item.message}`) : []);
    toast.show(errorMessage(error, t('errors.generic')), 'error');
  };

  const save = async () => {
    setBusy(true);
    setErrors([]);
    try {
      const saved = await adminApi.saveSettings(settingsKey, draft);
      queryClient.setQueryData(['admin', 'settings', settingsKey], saved);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'offers'] });
      toast.show(t('admin.saved'));
    } catch (error) {
      handle(error);
    } finally {
      setBusy(false);
    }
  };

  const preview = async () => {
    if (settingsKey !== 'pricing' && settingsKey !== 'loyalty') {
      return;
    }
    setBusy(true);
    setErrors([]);
    try {
      setImpact(await adminApi.impactPreview(settingsKey, draft));
    } catch (error) {
      handle(error);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="stack">
      <p className="small muted">{t('admin.settings.jsonHint')}</p>
      <div className="card">
        <SettingsFields value={draft} path={[]} onChange={(path, value) => setDraft(setAt(draft, path, value))} />
      </div>
      {errors.length > 0 && (
        <ul className="alert alert-danger" style={{ display: 'block', paddingLeft: 32 }}>
          {errors.map((error) => (
            <li key={error}>{error}</li>
          ))}
        </ul>
      )}
      {impact && (
        <div className={`alert ${impact.failingOffers.length > 0 ? 'alert-warning' : 'alert-success'}`}>
          <div>
            <p>{t('admin.settings.impactResult', { count: impact.affectedOffers })}</p>
            <p className="caption">
              {impact.failingOffers.length > 0 ? t('admin.settings.failing', { offers: impact.failingOffers.join(', ') }) : t('admin.settings.noFailing')}
            </p>
          </div>
        </div>
      )}
      <div className="row wrap">
        {(settingsKey === 'pricing' || settingsKey === 'loyalty') && (
          <button type="button" className="btn btn-secondary" onClick={preview} disabled={busy}>
            {t('admin.settings.impact')}
          </button>
        )}
        <button type="button" className="btn btn-primary" onClick={save} disabled={busy}>
          {busy ? t('common.saving') : t('common.save')}
        </button>
      </div>
    </div>
  );
}

export default function AdminSettingsPage() {
  const { t } = useTranslation();
  const [key, setKey] = useState<SettingsKey>('pricing');
  return (
    <>
      <AdminHeader title={t('admin.nav.settings')} />
      <div style={{ marginBottom: 16, overflowX: 'auto' }}>
        <Segmented<SettingsKey>
          label={t('admin.nav.settings')}
          value={key}
          onChange={setKey}
          options={[
            { value: 'pricing', label: t('admin.settings.pricing') },
            { value: 'loyalty', label: t('admin.settings.loyalty') },
            { value: 'membership', label: t('admin.settings.membership') },
            { value: 'recommendation', label: t('admin.settings.recommendation') },
          ]}
        />
      </div>
      <SettingsEditor key={key} settingsKey={key} />
    </>
  );
}
