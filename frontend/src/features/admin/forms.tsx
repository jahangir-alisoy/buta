import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { ApiError } from '../../shared/api/client';
import type { LocalizedText } from './types';

const BAKU_OFFSET_MS = 4 * 60 * 60 * 1000;

export function toLocalInput(iso: string | undefined | null): string {
  if (!iso) {
    return '';
  }
  return new Date(new Date(iso).getTime() + BAKU_OFFSET_MS).toISOString().slice(0, 16);
}

export function fromLocalInput(value: string): string | undefined {
  if (!value) {
    return undefined;
  }
  const parsed = new Date(`${value}:00+04:00`);
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString();
}

export function optionalNumber(value: string): number | undefined {
  return value.trim() === '' ? undefined : Number(value);
}

export function emptyText(): LocalizedText {
  return { az: '', en: '' };
}

export function optionalText(text: LocalizedText | undefined): LocalizedText | undefined {
  return text && (text.az.trim() || text.en.trim()) ? text : undefined;
}

export function useLocalized() {
  const { i18n } = useTranslation();
  return (text: LocalizedText | undefined | null) => (text ? (i18n.language === 'en' ? text.en : text.az) || text.en || text.az : '');
}

export function fieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) {
    return {};
  }
  return Object.fromEntries(error.fieldErrors.map((item) => [item.field, item.message]));
}

interface LocalizedInputProps {
  id: string;
  label: string;
  value: LocalizedText;
  onChange: (value: LocalizedText) => void;
  multiline?: boolean;
  maxLength?: number;
  error?: string;
}

export function LocalizedInput({ id, label, value, onChange, multiline, maxLength = 200, error }: LocalizedInputProps) {
  const { t } = useTranslation();
  const render = (language: 'az' | 'en') => {
    const props = {
      id: `${id}-${language}`,
      value: value[language],
      maxLength,
      'aria-label': `${label} (${language.toUpperCase()})`,
      onChange: (event: { target: { value: string } }) => onChange({ ...value, [language]: event.target.value }),
    };
    return multiline ? <textarea className="textarea" rows={3} {...props} /> : <input className="input" {...props} />;
  };
  return (
    <div className="field span-all">
      <span className="label">{label}</span>
      <div className="form-grid">
        <label className="stack" style={{ gap: 4 }}>
          <span className="caption tertiary">{t('admin.langAz')}</span>
          {render('az')}
        </label>
        <label className="stack" style={{ gap: 4 }}>
          <span className="caption tertiary">{t('admin.langEn')}</span>
          {render('en')}
        </label>
      </div>
      {error && <span className="field-error">{error}</span>}
    </div>
  );
}

export function FormField({ id, label, hint, error, children, wide }: { id: string; label: string; hint?: string; error?: string; children: ReactNode; wide?: boolean }) {
  return (
    <div className={`field${wide ? ' span-all' : ''}`}>
      <label htmlFor={id}>{label}</label>
      {children}
      {error ? <span className="field-error">{error}</span> : hint && <span className="field-hint">{hint}</span>}
    </div>
  );
}

export function CheckField({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return (
    <label className="checkbox">
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      {label}
    </label>
  );
}

export function AdminHeader({ title, action }: { title: string; action?: ReactNode }) {
  return (
    <header className="row spread wrap" style={{ marginBottom: 16 }}>
      <h1 className="section-title" style={{ fontSize: 'var(--text-headline)' }}>
        {title}
      </h1>
      {action}
    </header>
  );
}

export function StatusPill({ status }: { status: string }) {
  const { t } = useTranslation();
  const tone = ['PUBLISHED', 'ACTIVE', 'QUALIFIED'].includes(status)
    ? 'badge-success'
    : ['PAUSED', 'UNDER_REVIEW', 'PENDING', 'SUSPENDED'].includes(status)
      ? 'badge-warning'
      : 'badge-soft';
  return <span className={`badge ${tone}`}>{t(`admin.statuses.${status}`, { defaultValue: status })}</span>;
}
