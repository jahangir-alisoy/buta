import { useRef, type ChangeEvent, type ClipboardEvent, type KeyboardEvent, type ReactNode } from 'react';
import { Minus, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useGlassLens } from './useGlassLens';

interface FieldProps {
  id: string;
  label: string;
  hint?: string;
  error?: string;
  children: ReactNode;
}

export function Field({ id, label, hint, error, children }: FieldProps) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children}
      {error ? (
        <span className="field-error" id={`${id}-error`}>
          {error}
        </span>
      ) : (
        hint && (
          <span className="field-hint" id={`${id}-hint`}>
            {hint}
          </span>
        )
      )}
    </div>
  );
}

export function OtpInput({ value, onChange, disabled }: { value: string; onChange: (value: string) => void; disabled?: boolean }) {
  const refs = useRef<Array<HTMLInputElement | null>>([]);
  const { t } = useTranslation();
  const digits = Array.from({ length: 6 }, (_, index) => value[index] ?? '');

  const update = (index: number, digit: string) => {
    const next = digits.slice();
    next[index] = digit;
    onChange(next.join('').slice(0, 6));
  };

  const handleChange = (index: number) => (event: ChangeEvent<HTMLInputElement>) => {
    const cleaned = event.target.value.replace(/\D/g, '');
    if (cleaned.length > 1) {
      onChange(cleaned.slice(0, 6));
      refs.current[Math.min(cleaned.length, 5)]?.focus();
      return;
    }
    update(index, cleaned);
    if (cleaned && index < 5) {
      refs.current[index + 1]?.focus();
    }
  };

  const handleKey = (index: number) => (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Backspace' && !digits[index] && index > 0) {
      refs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (event: ClipboardEvent<HTMLInputElement>) => {
    const pasted = event.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (pasted) {
      event.preventDefault();
      onChange(pasted);
      refs.current[Math.min(pasted.length, 5)]?.focus();
    }
  };

  return (
    <div className="otp-input" role="group" aria-label={t('auth.code')}>
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(element) => {
            refs.current[index] = element;
          }}
          value={digit}
          onChange={handleChange(index)}
          onKeyDown={handleKey(index)}
          onPaste={handlePaste}
          inputMode="numeric"
          autoComplete={index === 0 ? 'one-time-code' : 'off'}
          pattern="[0-9]*"
          maxLength={6}
          disabled={disabled}
          aria-label={`${t('auth.code')} ${index + 1}`}
          autoFocus={index === 0}
        />
      ))}
    </div>
  );
}

interface StepperProps {
  value: number;
  min?: number;
  max: number;
  onChange: (value: number) => void;
  disabled?: boolean;
}

export function Stepper({ value, min = 1, max, onChange, disabled }: StepperProps) {
  const { t } = useTranslation();
  return (
    <div className="stepper" role="group" aria-label={t('common.quantity')}>
      <button type="button" onClick={() => onChange(value - 1)} disabled={disabled || value <= min} aria-label="−">
        <Minus size={16} />
      </button>
      <output aria-live="polite">{value}</output>
      <button type="button" onClick={() => onChange(value + 1)} disabled={disabled || value >= max} aria-label="+">
        <Plus size={16} />
      </button>
    </div>
  );
}

interface SegmentedProps<T extends string> {
  options: { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
  label: string;
}

export function Segmented<T extends string>({ options, value, onChange, label }: SegmentedProps<T>) {
  const selected = options.findIndex((option) => option.value === value);
  const lens = useGlassLens<HTMLDivElement>(selected, (index) => onChange(options[index].value));
  return (
    <div ref={lens.trackRef} className={`segmented ${lens.trackClassName}`} role="group" aria-label={label} {...lens.handlers}>
      <span ref={lens.lensRef} className="glass-lens" aria-hidden="true" />
      {options.map((option, index) => (
        <button
          key={option.value}
          type="button"
          aria-pressed={index === selected}
          className={index === lens.hovered ? 'is-under-lens' : undefined}
          onClick={(event) => lens.select(index, event)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}

export function Switch({ checked, onChange, label, disabled }: { checked: boolean; onChange: (value: boolean) => void; label: string; disabled?: boolean }) {
  return (
    <span className="switch">
      <input type="checkbox" role="switch" checked={checked} aria-label={label} disabled={disabled} onChange={(event) => onChange(event.target.checked)} />
      <span aria-hidden="true" />
    </span>
  );
}
